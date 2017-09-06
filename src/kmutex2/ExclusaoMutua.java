/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmutex2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rodrigo
 */
public class ExclusaoMutua extends Thread{
    public static final int ACK_PERM = 1;
    public static final int ACK_NOPERM = 0;
    private enum estado{
        NOT_REQUEST,REQUESTING,CS
    }
    private final Processo processo;
    private int c;
    private estado state;
    private Map<Integer,No> using;
    private Map<Integer,No> pending;
    private Map<Integer,Boolean> esgotadoTimeout;
    private int last;
    private Map<Integer,Boolean> haveperm;
    private Map<Integer,TimeOut> timeOut;
    public ExclusaoMutua(Processo processo){
        this.processo = processo;
        c = 0;
        state = estado.NOT_REQUEST;
        haveperm = new HashMap<>();
        using = new HashMap<>();
        pending = new HashMap<>();
        esgotadoTimeout = new HashMap<>();
        processo.setFalty(new ArrayList<>());
        timeOut =  new HashMap<>();
    }
    @Override
    public void run(){
        requestResource();
        try{
            Thread.sleep(1000);
            new ReRequestResource().start();
        }catch(InterruptedException ex){
            
        }
    }
    public void esgotouTimeout(int id){
        esgotadoTimeout.put(id, Boolean.TRUE);
    }
    
    public void requestResource(){
        using = new HashMap<>();
        state = estado.REQUESTING;
        last = c+1;
        for (No no : processo.getNos()) {
            haveperm.put(no.getPorta(), Boolean.FALSE);
        }
        for (No no : processo.getNos()){
            if(!processo.verificaFalty(no)){
                processo.enviaMensagem(no.getIp(), no.getPorta(),"n"+processo.getPorta()+"---request---"+last+getFalty());
                timeOut.put(no.getPorta(),new TimeOut(processo,no));
                timeOut.get(no.getPorta()).start();
            }
        }
    }
    
    public class ReceiveRequest extends Thread{
        private final String ip;
        private final int porta,lasti;
        private final ArrayList<No> noFalty;
        public ReceiveRequest(String ip,int porta,int last,ArrayList<Integer> falty){
            this.ip = ip;
            this.porta = porta;
            this.lasti = last;
            noFalty = new ArrayList<>();
            for (Integer falty1 : falty) {
                noFalty.add(processo.getNo(falty1));
            }
        }
        @Override
        public void run(){
            //Cancela o timeout do processo pj
            TimeOut t = timeOut.get(porta);
            if(t!=null){
                t.cancelarTimeout();
                timeOut.remove(porta);
            }
            c = (c>lasti?c:lasti) + 1;
            //União dos conjuntos
            for (No falty1 : noFalty) {
                processo.adicionaFalty(falty1);
            }
            //Remoção do haveperm
            for (No falty1 : processo.getFalty()) {
                if(haveperm.get(falty1.getPorta()))
                    haveperm.replace(falty1.getPorta(), Boolean.FALSE);
            }
            //Verifica se o processo atual está na região crítica ou se o relógio
            //Do solicitante é maior que o seu
            if((state==estado.CS)||((state==estado.REQUESTING)&&((processo.getPorta()<porta)&&(last<=lasti)))){
                //Envia o NOPERM
                processo.enviaMensagem(ip, porta, "n"+processo.getPorta()+"---reply---0"+getFalty());
                //Adiciona o no ao conjunto de pending
                pending.put(porta, processo.getNo(porta));
            }else{
                //Envia o PERM
                processo.enviaMensagem(ip, porta, "n"+processo.getPorta()+"---reply---1"+getFalty());
            }
        }
    }
    
    public class ReRequestResource extends Thread{
        @Override
        public void run(){
            if(using.size()>0){
                for (No no : processo.getNos()) {
                    //Envia para todos os nos que estão
                    if((!processo.verificaFalty(no))&&(using.get(no.getPorta())!=null)){
                        processo.enviaMensagem(no.getIp(), no.getPorta(),"n"+processo.getPorta()+"---request---"+last+getFalty());
                        timeOut.put(no.getPorta(),new TimeOut(processo,no));
                        timeOut.get(no.getPorta()).start();
                    }
                }
            }
        }
    }
    
    public class ReplyReceiver extends Thread{
        private final String ip;
        private final int porta;
        private final int ack;
        private final ArrayList<Integer> falty;
        private final ArrayList<No> noFalty;
        public ReplyReceiver(String ip,int porta,int ack,ArrayList<Integer> falty){
            this.ip = ip;
            this.porta = porta;
            this.ack = ack;
            this.falty = falty;
            noFalty = new ArrayList<>();
            //Converte os ids em nos
            for (Integer falty1 : falty) {
                noFalty.add(processo.getNo(falty1));
            }
        }
        @Override
        public void run(){
            //Adiciona o falty recebido ao falty do processo
            for (No falty1 : noFalty) {
                processo.adicionaFalty(falty1);
            }
            //Busca o nó
            No no = processo.getNo(porta);
            if((state==estado.REQUESTING)&&(ack==ACK_PERM)&&(!processo.verificaFalty(no))){
                //timeOut.get(porta).cancelarTimeout();
                //timeOut.remove(porta);
                haveperm.put(porta,Boolean.TRUE);
            }else if((state==estado.REQUESTING)&&(ack==ACK_NOPERM)&&(!processo.verificaFalty(no))){
                using.put(porta,no);
            }else{
                using.remove(porta);
                //timeOut.get(porta).cancelarTimeout();
                //timeOut.remove(porta);
            }
            if(haveperm.size()>=processo.getNos().size()-processo.getFalty().size()-processo.getRecursos()){
                int utilizando = 0;
                for (No no1 : processo.getNos()) {
                    if((!processo.verificaFalty(no1))&&(!haveperm.get(no1.getPorta()))){
                        utilizando++;
                    }
                }
                if(utilizando<processo.getRecursos()){
                    System.out.println("Thread "+processo.getPorta()+": entrou na região crítica");
                    state = estado.CS;
                    try{
                        System.out.println("Thread "+processo.getPorta()+": Consumindo...");
                        Thread.sleep(1000);
                    }catch(InterruptedException ex){

                    }
                    new ReleaseResource().start();
                    System.out.println("Thread "+processo.getPorta()+": Liberando recurso!");
                }
            }
        }
    }
    
    public class ReleaseResource extends Thread{
        @Override
        public void run(){
            for (No no : processo.getNos()) {
                if((!processo.verificaFalty(no))&&(pending.get(no.getPorta())!=null)){
                    processo.enviaMensagem(no.getIp(), no.getPorta(), "n"+processo.getPorta()+"---reply---1"+getFalty());
                }
            }
            pending = new HashMap<>();
            state = estado.NOT_REQUEST;
            System.out.println("Thread "+processo.getPorta()+": Saiu da região crítica");
        }
    }
    
    public class NotificationReceiver extends Thread{
        private final ArrayList<Integer> falty;
        public NotificationReceiver(ArrayList<Integer> falty){
            this.falty = falty;
        }
        @Override
        public void run(){
            ArrayList<No> noFalty = new ArrayList<>();
            //Converte os ids em nos
            for (Integer falty1 : falty) {
                noFalty.add(processo.getNo(falty1));
            }
            for (No falty1 : noFalty) {
                processo.adicionaFalty(falty1);
            }
        }
    }
    
    public String getFalty(){
        String faltyString = "";
        for (No falty : processo.getFalty()) {
            faltyString += "---"+falty.getPorta();
        }
        return faltyString;
    }
}
