/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmutex2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Rodrigo
 */
public class Recebedor extends Thread{
    private InputStream cliente;
    private Processo processo;
    private String ipCliente;
    public Recebedor(InputStream cliente,String ipCliente,Processo processo) {
        this.cliente = cliente;
        this.processo = processo;
        this.ipCliente = ipCliente;
    }

    @Override
    public void run() {
        Scanner s = new Scanner(this.cliente);
        while (s.hasNextLine()) {
            //TRATAR TODAS AS MENSAGENS DE ACORDO COM O PADRÃO CRIADO PARA:
            ArrayList<Integer> falty;
            String msg = s.nextLine();
            System.out.println("Recebedor "+processo.getPorta()+": recebeu "+msg);
            String[] partes = msg.split("---");
            int nProcesso = Integer.parseInt(partes[0].split("n")[1]);
            No no = processo.getNo(ipCliente, nProcesso);
            switch(partes[1]){
                case "request":
                    /*  Parâmetros esperados:
                        partes[2] = lasti
                        partes[3..n] = falty
                    */
                    falty = new ArrayList<>();
                    for(int i = 3;i<partes.length;i++){
                        falty.add(Integer.valueOf(partes[i]));
                    }
                    processo.getExclusaoMutua().new ReceiveRequest(ipCliente,nProcesso,Integer.valueOf(partes[2]), falty).start();
                break;
                case "reply":
                    /*  Parâmetros esperados:
                        partes[2] = ack
                        partes[3...n] = falty
                    */
                    falty = new ArrayList<>();
                    for(int i = 3;i<partes.length;i++){
                        falty.add(Integer.valueOf(partes[i]));
                    }
                    processo.getExclusaoMutua().new ReplyReceiver(ipCliente,nProcesso,Integer.valueOf(partes[2]), falty).start();
                break;
                case "notification":
                    /*  Parâmetros esperados:
                        partes[2...n] = ip */
                    falty = new ArrayList<>();
                    for(int i = 2;i<partes.length;i++){
                        falty.add(Integer.valueOf(partes[i]));
                    }
                    processo.getExclusaoMutua().new NotificationReceiver(falty).start();
                break;
                default:
                    System.out.println("Erro! Mensagem recebida: "+partes[1]);
            }
        }
        s.close();
    }
}
