
package com.amz.jde.cltOC;

import net.rim.device.api.io.*;

public class Global{ //TODO: NO deber�amos usar Globals sino pasar par�metros...
    public static DatagramConnectionBase conn; 
    public static int connFlag = 0;
    public static int connAtendidoFlag = 0;
    public static int initPhase = 1; //Inicializamos en la cantidad de fases!!!
    public static String svrPort = "";
    public static String cltPort = "";
    public static String[] productoCodArr;
    
}
