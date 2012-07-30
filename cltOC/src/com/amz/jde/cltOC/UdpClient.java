/*
 * UdpClient.java 
 *
 */
 
package com.amz.jde.cltOC;

import com.amz.jde.cltOC.ScreenClient;
import com.amz.jde.cltOC.ScreenClientScreen;
import com.amz.jde.cltOC.ScreenClientScreen_Options;

import java.io.*; 
import javax.microedition.io.*;

import net.rim.device.api.ui.*;
import net.rim.device.api.io.*;
import net.rim.blackberry.api.phone.Phone;

/**
 * This class represents the client in our client/server configuration.
 */
public final class UdpClient extends Thread {    
    private String msg, host, connParm, phoneNbr;
    private ScreenClientScreen screen;
    private ScreenClientScreen_Options.WaitMessageScreen screenW;
    private ScreenClient app;
    private byte [ ] bufOut, bufIn;
    
    //Constructor
    /**
     * Creates a new UdpClient.
     * @param msg	The message sent to the server.
     * @param host	The server.
     * @param connParm	Wi-Fi/APN connection.
     */
    public UdpClient(String msg1, String host1, String connParm1) {
		
        phoneNbr = Phone.getDevicePhoneNumber(false);
        msg = msg1;
        host = host1; 
        connParm = connParm1;            
        app = (ScreenClient)UiApplication.getUiApplication();
        screen = app.getScreen(); 
        
        app.invokeAndWait(new Runnable(){
            public void run () {
            	screenW = new ScreenClientScreen_Options.WaitMessageScreen("Espere un momento...");  
            	app.pushScreen(screenW);
            }
        } );
    }
    
    /**
     * Implementation of Thread
     */
    public void run() {
  		
        try {
            //Make a UDP(datagram) connection to the server address (@parm host). 
            //El cliente primero se conecta al servidor despachador especificado en la configuración
        	//de este cliente (svrDispatcher) con el puerto remoto 49153 y escucha por el 49152.  
        	//Con esto, recibe del servidor despachador (svrDispatcher) la IP del servidor JDE (svrWorkerOC),
        	//el puerto remoto y el puerto de escucha local...  
        	//Así, entonces, el cliente se conecta al svrWorker asignado y somete las transacciones a JDE.
        	
            //Display the status message on the event thread.
        	
        	int unaVez = 0;
        	
        	while (Global.connAtendidoFlag == 0 || 
        			((Global.connAtendidoFlag == 1) && (Global.initPhase == 1)) ||
        			((Global.connAtendidoFlag == 1) && (Global.initPhase == 0) && (unaVez == 0))
        			){	
        		
        		unaVez++;
        	       	       	       		            
	            if (Global.connFlag == 0) { //El puerto está cerrado, podemos abrirlo...
	                if (Global.connAtendidoFlag == 0) {//NO atendido: hay que negociar el servidor...
	                    Global.conn = (DatagramConnectionBase)Connector.open("datagram://" + host + ":49153;49152" + 
	                    				connParm, 3, true);   
	                    Global.conn.setTimeout(20000); //TimeOut de 20 segundos...
	                    Global.connFlag ++; //Ya el puerto no está cerrado...
	                    
	                    //Display the status message on the event thread.
	                    app.invokeLater(new Runnable() {
	                        public void run() {
	                            screen.updateStatus("Conexion svrDispatcher... " );          
	                        }   
	                    });
	                     
	                 } else { //Ya estamos atendidos por el svrDispatcher y conocemos al svrWorker: nos conectamos entonces...
	                    Global.conn = (DatagramConnectionBase)Connector.open("datagram://" + host + ":" + Global.svrPort + ";" + 
	                                    Global.cltPort + connParm, 3, true);
	                    Global.conn.setTimeout(40000); //TimeOut de 40 segundos...
	                    Global.connFlag ++; //Ya el puerto no está cerrado...
	                                    
	                    //Display the status message on the event thread.
	                    app.invokeLater(new Runnable() {
	                        public void run() {
	                            screen.updateStatus("Conexion svrWorker... " );          
	                        }   
	                    });               
	                                    
	                 } ;        
	            } else Global.connFlag ++;  //El puerto ya estaba abierto; así que lo reutilizamos......
	            
	            int mensajeTerminado = 0; 
	            int indiceProductoArr = 1;
	            while (((mensajeTerminado == 0) || (Global.initPhase == 1)) && (Global.connFlag > 0)) { 
		            //Convert the message to a byte array for sending.
	            	String msgBuf1 = "XXX:";
		            final String msgBuf;
		            if (Global.connAtendidoFlag == 0) {
		            	msgBuf1 = phoneNbr;
		                bufOut = phoneNbr.getBytes(); //El mensaje es el Nro.Cel...
		            }else {
		            	if (Global.initPhase == 1){ //Procesamiento de inicialización... 
		            		msgBuf1 = "T01:" ; //Transacción T01 = lista de productos... 
		            		bufOut = msgBuf1.getBytes(); //El mensaje...
		            	}
		            	if (Global.initPhase == 0){ //Procesamiento normal...
			            	msgBuf1 = "T11:" + msg; //Transacción T11 = pantalla datos OC...
			                bufOut = msgBuf1.getBytes(); //El mensaje son los campos de la forma...
		            	}
		            }
		            
		            //Create a datagram and send it across the connection.
		            Datagram outDatagram = Global.conn.newDatagram(bufOut, bufOut.length);          
		            Global.conn.send(outDatagram);
		            
	                try{
	                    //Sleep for the next 7 secs....
	                    sleep(7000);
	                }
	                catch (InterruptedException iex){} //Couldn't sleep.
	                
		                    
		            //Expect a response...
		            bufIn = new byte[7168];            
		            Datagram inDatagram = Global.conn.newDatagram(bufIn, bufIn.length);
		            Global.conn.receive(inDatagram);           
		            final String response = new String(inDatagram.getData());
		            
		            if (Global.connAtendidoFlag == 0) { //NO Atendido... Vamos a ver qué respondió el servidor...	            	
		            	mensajeTerminado = 1; //Esto es todo lo que debemos recibir...
		                Global.svrPort = response.substring(0,5); //El puerto del svrWorker...
		                Global.cltPort = response.substring(5,10); //El puerto local para escuchar ...
		                
		                //TODO: Validar los puertos >=55500...
		                
		                Global.connAtendidoFlag = 1; //Estamos Atendidos!!!
		                //Display the status message on the event thread.
		                
		                if (Global.connFlag > 0) Global.connFlag --;
		                //Close the connection
		                if (Global.connFlag == 0) {
		                    try{
		                    	Global.conn.close(); 
		                    } catch ( NullPointerException e){
		                    	//Se produce un error si el puerto nunca se abrió...
		                    }
		                }
		                app.invokeLater(new Runnable() {
		                    public void run() {
		                        screen.updateStatus("C O N E C T A D O... " );          
		                    }   
		                });
		            }
		            else{ //SI Atendido... Estamos conversando con el svrWorker...
		            	if (Global.initPhase == 1){//Estamos recibiendo data para la inicializacion...           		
		            		//if (response.substring(0,4) == "T01:"){ //Respuesta Transacción T01...
	            			//El mensaje es de la forma Txx:mnn con 
	            			//xx = código transacción
	            			//m = 1 indica que es el último mensaje
	            			//nn indica que hay nn+1 elementos en este mensaje      			
	            			for (int i = 0; i <  Integer.valueOf(response.substring(5,7)).intValue(); i++){
	            				Global.productoCodArr[indiceProductoArr] = response.substring(27 + (i * 50),57 + (i * 50)) + ":" + response.substring(7 + (i * 50),27 + (i * 50)) + "  ";          				
	            				indiceProductoArr++;
	            			}
	            			if (response.substring(4,5).equals("1")){
	            				mensajeTerminado = 1;
	            				Global.initPhase--; //Una inicialización menos...
	            				
	            				//Display the status message on the event thread.
	            		        app.invokeLater(new Runnable() {
	            		            public void run() {
	            		            	screen.updateDataSet();
	            		                screen.updateStatus("Data set actualizado");          
	            		            }
	            		        });	            		        
	            			}	            					            			
		            	}
		            	else{//Estamos recibiendo data de transacciones...
		            		mensajeTerminado = 1; //Esto es todo lo que debemos recibir...
		            	}	
		            }
	            } //While (((mensajeTerminado == 0) || (Global.initPhase == 1)) && (Global.connFlag > 0))...
        	} //while (Global.connAtendidoFlag == 0) || ...
        }
        catch(InterruptedIOException  ioe) { 
            final String error = ioe.toString();
            
            //Display the error message on the event thread. 
            app.invokeLater(new Runnable() {//TODO: El msg se debe poder mostrar en ScreenClientScreen_Options (PopUp??)...
                public void run() {
                    screen.updateStatus("E#11: " + error);          
                }                
            });
            
            if (Global.initPhase == 1){
				//Display the status message on the event thread.
		        app.invokeLater(new Runnable() {
		            public void run() {
		            	screen.updateDataSet();
		                screen.updateStatus("Data set actualizado con error...");          
		            }
		        });
		        Global.initPhase--; //Una inicialización menos...
            }
            
            try {
				sleep(3000);
			} catch (InterruptedException e) {
				//TODO Auto-generated catch block
			}
            
            if (app.getActiveScreen().equals(screenW)){
                app.invokeAndWait(new Runnable() {
                    public void run() {
                        app.popScreen(screenW);
                    }                
                });            
            }
        }
        catch(IOException   ioe) { 
            final String error = ioe.toString();
            
            //Display the error message on the event thread. 
            app.invokeLater(new Runnable() {//TODO: El msg se debe poder mostrar en ScreenClientScreen_Options (PopUp??)...
                public void run() {
                    screen.updateStatus("E#12: " + error);          
                }                
            });
            
            if (Global.initPhase == 1){
				//Display the status message on the event thread.
		        app.invokeLater(new Runnable() {
		            public void run() {
		            	screen.updateDataSet();
		                screen.updateStatus("Data set actualizado con error...");          
		            }
		        });
		        Global.initPhase--; //Una inicialización menos...
            }            
            
            try {
				sleep(3000);
			} catch (InterruptedException e) {
				//TODO Auto-generated catch block
			}
            
            if (app.getActiveScreen().equals(screenW)){
                app.invokeAndWait(new Runnable() {
                    public void run() {
                        app.popScreen(screenW);
                    }                
                });            
            }
        }
        finally { 
            if (app.getActiveScreen().equals(screenW)){    
                app.invokeAndWait(new Runnable() {
                    public void run() {
                        app.popScreen(screenW);
                    }                
                }); 
            }
            try {               
                if (Global.connFlag > 0) Global.connFlag --;
                //Close the connection
                if (Global.connFlag == 0) {
                    try{
                    	Global.conn.close(); 
                    } catch ( NullPointerException e){
                    	//Se produce un error si el puerto nunca se abrió...
                    }
                }
            }
            catch(IOException ioe) {  
                final String error = ioe.toString();
                
                //Display the error message on the event thread. 
                app.invokeLater(new Runnable() {
                    public void run() {
                        screen.updateStatus("E#13: " + error);          
                    }                
                });              
            }     
        }                	
    }     
}
