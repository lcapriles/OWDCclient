/*
 * ScreenClient.java
 */

package com.amz.jde.cltOC;

import java.lang.Integer;
import java.util.*;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.collection.util.*;
import net.rim.device.api.system.*;

public final class ScreenClient extends UiApplication {   
    private ScreenClientScreen _screen;
    
    /**
     * Entry point for application. 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        //Create a new instance of the application and make the currently
        //running thread the application's event dispatch thread.
    	
        Global.productoCodArr = new String[2000];                                  
//      Global.productoCodArr[0] = "123456789012345678901234567890" + ":12345678901234567890";      
        Global.productoCodArr[0] = "                              " + "                    ";
        
        ScreenClient theApp = new ScreenClient();      
        theApp.enterEventDispatcher(); 
    }    
    
    //Constructor
    public ScreenClient() {
    	String _connStr;
    	
        //Create our main screen and push it onto the UI stack.
        _screen = new ScreenClientScreen();        
        pushScreen(_screen); 
        
        //Nos conectamos con los valores guardados... 
        Integer _connectionRB = (Integer)ScreenClientScreen_Options.AppOptions.getItem("conn");
        String _hostField = (String)ScreenClientScreen_Options.AppOptions.getItem("host");       
        if (_connectionRB.intValue() == 0) {
            _connStr = ";interface=wifi"; //Conexión Wi-Fi...
        } else{
            _connStr = ";deviceside=true"; //Conexión APN...
        };
        UdpClient client = new UdpClient("Conectando...", _hostField, _connStr); 
        client.start();
    }     
    
    /**
     * Provides access to the UI screen.
     * @return The UI screen.
     */
    ScreenClientScreen getScreen() {
        return _screen;
    }  
}


/**
 * This MainScreen class provides standard GUI behavior. 
 */
final class ScreenClientScreen extends MainScreen implements FieldChangeListener {
    private EditField _clienteField, _cantidadField;
    private BasicFilteredList _productoList;
    private AutoCompleteField _productoField;
    private String _hostField, _productoCodField;
    private LabelField _statusField;
    private Integer _connectionRB;
    private ButtonField _sendButton;
    private ButtonField _okButton;
    private StringBuffer _status;
    
    //Constructor
    ScreenClientScreen() { //TODO: Esto es horrendo??... hay que acomodar??...      
       _status = new StringBuffer();
    
        //Initialize UI components.
        setTitle("Compras Demo"); 
        
      //Colocamos los campos...
        ScreenClientVFM vfm = new ScreenClientVFM();
        _clienteField = new EditField("Cliente..","",64, FIELD_TOP); 
        vfm.add(_clienteField);     
        _productoList = new BasicFilteredList();
        _productoList.addDataSet(1,Global.productoCodArr,"productos",BasicFilteredList.COMPARISON_IGNORE_CASE);
        _productoField = new AutoCompleteField(_productoList);
        //_productoField.setKeystrokeForwarding(true);
        LabelField etiqueta1 = new LabelField("Producto..");
        HorizontalFieldManager hfm1 = new HorizontalFieldManager();
        hfm1.add(etiqueta1);
        hfm1.add(_productoField);
        vfm.add(hfm1);        
        _cantidadField = new EditField("Cantidad.","",64, FIELD_TOP);               
        vfm.add(_cantidadField);      
        add(vfm);
        
        HorizontalFieldManager hfm2 = new HorizontalFieldManager(Field.FIELD_HCENTER);
        _sendButton = new ButtonField("Send");
        _sendButton.setChangeListener(this);               
        _okButton = new ButtonField("OK");   
        _okButton.setChangeListener(this);            
        hfm2.add(_sendButton);
        hfm2.add(_okButton);
        add(hfm2);
            
        _statusField = new LabelField();          
        add(_statusField);
                      
        addMenuItem(_sendItem);
        addMenuItem(_okItem);
        addMenuItem(_connectItem);
        addMenuItem(_optionsItem);
        
        _connectionRB = (Integer)ScreenClientScreen_Options.AppOptions.getItem("conn");
        _hostField = (String)ScreenClientScreen_Options.AppOptions.getItem("host");
    }     
    
    /**
     * A customized VerticalFieldManager.
     */            
    private static class ScreenClientVFM extends VerticalFieldManager {
        //Constructor
        ScreenClientVFM() {
            super(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR);                                
        }
    }    
    
    /**
     * Creates a new client thread.
     */    
    private void createClient() {
        String _connStr, _mensajeStr;  
        
        //Our UdpClient class needs to be run in a separate thread as
        //blocking operations are not permitted on event dispatch thread.
        
        if (_connectionRB.intValue() == 0) {
            _connStr = ";interface=wifi"; //Conexión Wi-Fi...
        } else{
            _connStr = ";deviceside=true"; //Conexión APN...
        };
        
        _mensajeStr = _clienteField.getText() + ";" + _productoCodField + ";" + _cantidadField.getText();
        
        UdpClient client = new UdpClient(_mensajeStr, _hostField, _connStr);  
        client.start();
        
        _productoField.getEditField().setText(Global.productoCodArr[0]);
     }    
    
    /**
     *  @see net.rim.device.api.ui.MainScreen#onSavePrompt() 
     */
    protected boolean onSavePrompt() {        
        return true;
    } 
    
    /**
     * We are implementing this method to intercept property changes on the send and clear buttons.
     * 
     * @see FieldChangeListener#fieldChanged(Field, int)
     */
    public void fieldChanged(Field field, int context) {
        if(field == _sendButton) {
            handleSend();
        }
        if(field == _okButton) {
            handleOK();
        } 
    }   
    
    //This menu item provides an alternative to clicking the send button.
    private MenuItem _sendItem = new MenuItem("Send", 11000, 0) {      
        public void run() {            
            handleSend();
        }
    };
    
    //This menu item provides an alternative to clicking the clear button.
    private MenuItem _okItem = new MenuItem("OK", 11001, 0) {      
        public void run() {
            handleOK();
        }
    }; 
    
    //This menu item provides connect option.
    private MenuItem _connectItem = new MenuItem("Connect", 11010, 0) {      
        public void run() {
            handleConnect();
        }
    }; 
    
    //This menu item provides options option.
    private MenuItem _optionsItem = new MenuItem("Options", 11011, 0) {      
        public void run() {
            handleOptions();
        }
    };    
    
    /**
     * Handles a send button or send menu item click.
     */
    private void handleSend() {
        if(_productoField.isDirty()) {
        	_productoCodField =  _productoField.getEditField().getText(31, 20);
            createClient();            
            _cantidadField.setText("");
            _status.setLength(0);
        }
        else {
            Dialog.alert("Por favor, introduzca datos...");            
        } 
        _productoField.setFocus();        
    } 
    
    /**
     * Handles a clear button or clear menu item click.
     */
    private void handleOK() {       
        _statusField.setText("");
        _productoCodField = "999";
        _cantidadField.setText("");
        _status.setLength(0); 
        createClient();              
    }     
    
    /**
     * Handles connect menu item click.
     */
    private void handleConnect() {
    	 _statusField.setText("");
        _status.setLength(0); 
        createClient();              
    } 
    
    /**
     * Handles options menu item click.
     */
    private void handleOptions() {       
    	//Create options screen and push it onto the UI stack.
    	ScreenClientScreen_Options _screenO = new ScreenClientScreen_Options(); 
    	UiApplication.getUiApplication().pushScreen(_screenO);
    }     

	/**
     * Updates the status field.
     * @param text The text with which to update the status field.
     */
    void updateStatus(String text) {
        _status.append(text + '\n');
        _statusField.setText(_status.toString());
    }
    
    
    /**
     * @see net.rim.device.api.ui.Screen#invokeAction(int)
     */   
    protected boolean invokeAction(int action) {
        boolean handled = super.invokeAction(action); 
                    
        if(!handled) {
            switch(action) {
                case ACTION_INVOKE:  { //Trackball click.  
                    return true; //Suppress the menu
                }
            }
        }        
        return handled;                
    }
    
	/**
     * Updates the data set field.
     * @param none.
     */
    void updateDataSet() {
    	_productoList.addDataSet(2,Global.productoCodArr,"productos",BasicFilteredList.COMPARISON_IGNORE_CASE);
    }
}



final class ScreenClientScreen_Options extends MainScreen implements FieldChangeListener {
    private EditField 	_hostField;
    private LabelField 	_statusField;
    private RadioButtonGroup _connectionRB;
    private ButtonField _cancelButton;
    private ButtonField _okButton;
    private StringBuffer _status;
    
    private Hashtable settingsItemsHash;
    
    
    //Constructor
    ScreenClientScreen_Options() { //TODO: Esto es horrendo??... hay que acomodar??...      
       _status = new StringBuffer();
    
        //Initialize UI components.
        setTitle("Compras Demo - Opciones");
        
      //Colocamos los campos...
        ScreenClientVFM vm = new ScreenClientVFM();
        _connectionRB = new RadioButtonGroup();
        add(new RadioButtonField("Wi-Fi Conn...",_connectionRB,true));
        add(new RadioButtonField("APN Conn...",_connectionRB,false));
        //verticalManager.add(_connectionRB);
        _hostField = new EditField("Host.. ","",32, FIELD_TOP);               
        vm.add(_hostField);
        add(vm);
                
        HorizontalFieldManager hfm = new HorizontalFieldManager(Field.FIELD_HCENTER);
        _cancelButton = new ButtonField("Cancel");
        _cancelButton.setChangeListener(this);               
        _okButton = new ButtonField("OK");   
        _okButton.setChangeListener(this);            
        hfm.add(_cancelButton);
        hfm.add(_okButton);
        add(hfm);
            
        _statusField = new LabelField();          
        add(_statusField);
                      
        addMenuItem(_cancelItem);
        addMenuItem(_okItem); 
        
        settingsItemsHash = AppOptions.getItems();
        _connectionRB.setSelectedIndex(((Integer) settingsItemsHash.get("conn")).intValue());
        _hostField.setText((String) settingsItemsHash.get("host"));
          
    } 
    
    //Ventana Espera...

    static class WaitMessageScreen extends PopupScreen {

        public WaitMessageScreen (String msg){
      	
        	super(new HorizontalFieldManager(HorizontalFieldManager.VERTICAL_SCROLL | HorizontalFieldManager.VERTICAL_SCROLLBAR));
            GIFEncodedImage ourAnimation = (GIFEncodedImage) GIFEncodedImage.getEncodedImageResource("spinner.gif");
            AnimatedGIFField  _ourAnimation = new AnimatedGIFField(ourAnimation, Field.FIELD_LEFT);
            this.add(_ourAnimation);
            String msg1 = "    " + msg;
            LabelField  _ourLabelField = new LabelField(msg1, Field.FIELD_VCENTER);
            this.add(_ourLabelField);
        }
    }
        
    /**
     * Manejo de la persistencia de las opciones...
     */
	static final class AppOptions { 
		static PersistentObject store;
		static Hashtable settingsItems;
		
	    static {
	    	store = PersistentStore.getPersistentObject(0x835e99c3feb30d46L);//com.amz.jde.cltOC
	    	Hashtable temp = (Hashtable) store.getContents();
	    	if(temp == null) {
	    		settingsItems = new Hashtable();
	    		settingsItems.put("conn", new Integer(0));
	    		settingsItems.put("host", new String("localhost"));
	    		store.setContents(settingsItems);
	    		store.commit();
	         }
	    	else 
	    		settingsItems = temp;
	    }
	    
	    static void addItem(String itemName, Object itemValue) {
	    	settingsItems.put(itemName, itemValue);
	    }
	
	    static Hashtable getItems() {
	    	return settingsItems;
	    }
	
	    static Object getItem(String itemName) {
	    	return settingsItems.get(itemName);
	    }
	
	    static void persist() {
	    	synchronized (store) {
	    		store.setContents(settingsItems);
	    		store.commit();
	    	}
	    }
	}
    
    /**
     * A customized VerticalFieldManager.
     */            
    private static class ScreenClientVFM extends VerticalFieldManager {
        //Constructor
        ScreenClientVFM() {
            super(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR);                                
        }
    }     
    
    /**
     *  @see net.rim.device.api.ui.MainScreen#onSavePrompt() 
     */
    protected boolean onSavePrompt() {        
        return true;
    } 
    
    /**
     * We are implementing this method to intercept property changes on the send and clear buttons.
     * 
     * @see FieldChangeListener#fieldChanged(Field, int)
     */
    public void fieldChanged(Field field, int context) {
        if(field == _cancelButton) {
            handleCancel();
        }
        if(field == _okButton) {
            handleOK();
        }  
    }   
    
    //This menu item provides an alternative to clicking the send button.
    private MenuItem _cancelItem = new MenuItem("Cancel", 11000, 0) {      
        public void run() {            
            handleCancel();
        }
    };
    
    //This menu item provides an alternative to clicking the clear button.
    private MenuItem _okItem = new MenuItem("OK", 11001, 0) {      
        public void run() {
            handleOK();
        }
    };     
    
    /**
     * Handles a send button or send menu item click.
     */
    private void handleCancel() {
        _statusField.setText("");
        _status.setLength(0); 
        PersistentStore.destroyPersistentObject(0x835e99c3feb30d46L);//com.amz.jde.cltOC
        Dialog.alert("Presitent Store destruido!!! ");         
    } 
    
    /**
     * Handles a clear button or clear menu item click.
     */
    private void handleOK() {    
    	String _connStr;  
    	
        _statusField.setText("");
        _status.setLength(0);
        AppOptions.addItem("conn", new Integer(_connectionRB.getSelectedIndex()));
        AppOptions.addItem("host", new String(_hostField.getText()));
        AppOptions.persist();               
        
        if (_connectionRB.getSelectedIndex() == 0) {
            _connStr = ";interface=wifi"; //Conexión Wi-Fi...
        } else{
            _connStr = ";deviceside=true"; //Conexión APN...
        };
        
      //Nos conectamos con los nuevos valores... 
        UdpClient client = new UdpClient("Conentando...", _hostField.getText(), _connStr); 
        client.start();
    }        
    
    /**
     * @see net.rim.device.api.ui.Screen#invokeAction(int)
     */   
    protected boolean invokeAction(int action) {
        boolean handled = super.invokeAction(action); 
                    
        if(!handled) {
            switch(action) {
                case ACTION_INVOKE:  { //Trackball click.  
                    return true; //Suppress the menu
                }
            }
        }        
        return handled;                
    } 
}
