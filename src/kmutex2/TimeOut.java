/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmutex2;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author rodrigo
 */
class TimeOut extends Thread{
    private final No no;
    private final Processo processo;
    private ExecutorService executor;
    Future<String> future;
    public TimeOut(Processo processo,No no) {
        this.processo = processo;
        this.no = no;
    }
    
    @Override
    public void run(){
        executor = Executors.newSingleThreadExecutor();
        future = executor.submit(new Task1());
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
            throw new TimeoutException();
        } catch(CancellationException e){
            
        }catch (TimeoutException e){ //Caso o envio da mensagem venha com timeout
            //Failure Detector Task 2
            future.cancel(true);
            // Caso a tarefa falhe, adicionar o no atual ao conjunto falty 
            processo.getExclusaoMutua().esgotouTimeout(no.getPorta());
        } catch(Exception e){
            e.printStackTrace();
        }
        executor.shutdownNow();
        future = null;
    }
    //Failure Detector Task 1
    class Task1 implements Callable<String> {
        @Override
        public String call() throws Exception {
            //Emvia a mensagem areyoualive
            System.out.println("TimeOut "+processo.getPorta()+": Aguardando resposta de "+no.getPorta());
            //Aguarda 500 ms, tempo de resposta
            Thread.sleep(600);
            return "";
        }
    }
    
    public void cancelarTimeout(){
        if(future==null)
            return;
        if(future.isCancelled())
            return;
        future.cancel(true);
    }
}
