package client;

import java.io.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;




public class TheClient {

	private PassThruCardService servClient = null;
	boolean DISPLAY = true;
	boolean loop = true;

	static final byte CLA					= (byte)0x00;
	static final byte P1					= (byte)0x00;
	static final byte P2					= (byte)0x00;
	static final byte UPDATECARDKEY				= (byte)0x14;
	static final byte UNCIPHERFILEBYCARD			= (byte)0x13;
	static final byte CIPHERFILEBYCARD			= (byte)0x12;
	static final byte CIPHERANDUNCIPHERNAMEBYCARD		= (byte)0x11;
	static final byte READFILEFROMCARD			= (byte)0x10;
	static final byte WRITEFILETOCARD			= (byte)0x09;
	static final byte UPDATEWRITEPIN			= (byte)0x08;
	static final byte UPDATEREADPIN				= (byte)0x07;
	static final byte DISPLAYPINSECURITY			= (byte)0x06;
	static final byte DESACTIVATEACTIVATEPINSECURITY	= (byte)0x05;
	static final byte ENTERREADPIN				= (byte)0x04;
	static final byte ENTERWRITEPIN				= (byte)0x03;
	static final byte READNAMEFROMCARD			= (byte)0x02;
	static final byte WRITENAMETOCARD			= (byte)0x01;

	final static short MAXLENGTH = (short)126;
	static final byte P1_FILENAME 	 	= (byte)0x01;
	static final byte P1_BLOC 	 		= (byte)0x02;
	static final byte P1_VAR 	 		= (byte)0x03;
	static 	byte[] dataBlock = new byte[MAXLENGTH];
	public TheClient() {
		try {
			SmartCard.start();
			System.out.print( "Smartcard inserted?... " ); 

			CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 

			SmartCard sm = SmartCard.waitForCard (cr);

			if (sm != null) {
				System.out.println ("got a SmartCard object!\n");
			} else
				System.out.println( "did not get a SmartCard object!\n" );

			this.initNewCard( sm ); 

			SmartCard.shutdown();

		} catch( Exception e ) {
			System.out.println( "TheClient error: " + e.getMessage() );
		}
		java.lang.System.exit(0) ;
	}

	private ResponseAPDU sendAPDU(CommandAPDU cmd) {
		return sendAPDU(cmd, true);
	}

	private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
		ResponseAPDU result = null;
		try {
			result = this.servClient.sendCommandAPDU( cmd );
			if(display)
				displayAPDU(cmd, result);
		} catch( Exception e ) {
			System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return result;
	}


	/************************************************
	 * *********** BEGINNING OF TOOLS ***************
	 * **********************************************/


	private String apdu2string( APDU apdu ) {
		return removeCR( HexString.hexify( apdu.getBytes() ) );
	}


	public void displayAPDU( APDU apdu ) {
		System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
	}


	public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
		System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
		System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
	}


	private String removeCR( String string ) {
		return string.replace( '\n', ' ' );
	}


	/******************************************
	 * *********** END OF TOOLS ***************
	 * ****************************************/


	private boolean selectApplet() {
		boolean cardOk = false;
		try {
			CommandAPDU cmd = new CommandAPDU( new byte[] {
				(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
				    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
				    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
			} );
			ResponseAPDU resp = this.sendAPDU( cmd );
			if( this.apdu2string( resp ).equals( "90 00" ) )
				cardOk = true;
		} catch(Exception e) {
			System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
			java.lang.System.exit( -1 );
		}
		return cardOk;
	}


	private void initNewCard( SmartCard card ) {
		if( card != null )
			System.out.println( "Smartcard inserted\n" );
		else {
			System.out.println( "Did not get a smartcard" );
			System.exit( -1 );
		}

		System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


		try {
			this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
		} catch( Exception e ) {
			System.out.println( e.getMessage() );
		}

		System.out.println("Applet selecting...");
		if( !this.selectApplet() ) {
			System.out.println( "Wrong card, no applet to select!\n" );
			System.exit( 1 );
			return;
		} else 
			System.out.println( "Applet selected" );

		mainLoop();
	}


	void updateCardKey() {
	}


	void uncipherFileByCard() {
	}


	void cipherFileByCard() {
	}


	void cipherAndUncipherNameByCard() {
	}


	void readFileFromCard() {
	}


	void writeFileToCard() {
		System.out.println("Saisissez le fichier a ecrire sur la carte:");
		String filename = readKeyboard();
		byte filenameSize = (byte)filename.getBytes().length;
		int nbAPDUMax = 0;
		int lastAPDUsize = 0;

		/* envoi size filename et filename */
		System.out.println("==========Requete: Filename==========");
		byte[] header = {CLA,WRITEFILETOCARD, P1_FILENAME,P2}; // requete de type "filename" ( contient la taille de filename et filename)
		byte[] optional = new byte[(byte)1 + filenameSize];
		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		optional[0] = filenameSize;
		System.arraycopy(filename.getBytes(), (byte)0, optional, (byte)1, optional[0]);
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		System.arraycopy(optional,(byte)0,command,(byte)header.length,(byte)optional.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
		System.out.println("==========Fin Requete: Filename==========");
		/* end */

	
		try{
			DataInputStream filedata = new DataInputStream(new FileInputStream(filename));
		
		int return_value = 0;

		while( (return_value = filedata.read(dataBlock,0,MAXLENGTH)) !=-1 ) {
				System.out.println("return :"+return_value);
			if(return_value == MAXLENGTH){
				nbAPDUMax ++;

				System.out.println("Indice du bloc :"+(nbAPDUMax-1));
				short offset = (short)(((byte)1 + (byte)filename.getBytes().length + (byte)2) + ((byte)(nbAPDUMax-1) * (byte)MAXLENGTH));
				System.out.println("offset value: "+offset);


				/* envoi d'un bloc */
				System.out.println("==========Requete: Bloc==========");
				byte[] header1 = {CLA,WRITEFILETOCARD,P1_BLOC,(byte)(nbAPDUMax-1)}; // requete de type "bloc" (contient un bloc de 126 octets) avec P2 = indice du bloc
				byte[] optional1 = new byte[(byte)1 + (byte)return_value];
				byte[] command1 = new byte[(byte)header1.length + (byte)optional1.length];
				optional1[0] = (byte)return_value;
				System.arraycopy(dataBlock, (byte)0, optional1, (byte)1, optional1[0]);
				System.arraycopy(header1,(byte)0,command1,(byte)0,(byte)header1.length);
				System.arraycopy(optional1,(byte)0,command1,(byte)header1.length,(byte)optional1.length);
				CommandAPDU cmd1 = new CommandAPDU( command1);
				ResponseAPDU resp1 = this.sendAPDU( cmd1, DISPLAY );
				System.out.println("==========Fin Requete: Bloc==========");
				/* end */


			}else{

				lastAPDUsize = return_value;

				System.out.println("Indice du bloc :"+(nbAPDUMax));
				short offset = (short)(((byte)1 + (byte)8 + (byte)2) + ((byte)(nbAPDUMax) * (byte)MAXLENGTH));
				System.out.println("offset value: "+offset);



				/* envoi du DERNIER bloc */
				System.out.println("==========Requete: Last Bloc==========");
				byte[] header2 = {CLA,WRITEFILETOCARD,P1_BLOC,(byte)nbAPDUMax}; // requete de type "bloc" (contient un bloc de lastAPDUsize octets) avec P2 = indice du bloc
				byte[] optional2 = new byte[(byte)1 + (byte)lastAPDUsize];
				byte[] command2 = new byte[(byte)header2.length + (byte)optional2.length];
				optional2[0] = (byte)lastAPDUsize;
				System.arraycopy(dataBlock, (byte)0, optional2, (byte)1, optional2[0]);
				System.arraycopy(header2,(byte)0,command2,(byte)0,(byte)header2.length);
				System.arraycopy(optional2,(byte)0,command2,(byte)header2.length,(byte)optional2.length);
				CommandAPDU cmd2 = new CommandAPDU( command2);
				ResponseAPDU resp2 = this.sendAPDU( cmd2, DISPLAY );
				System.out.println("==========Fin Requete: Last Bloc==========");
				/* end */

				
				System.out.println("nbAPDUMax :"+nbAPDUMax+"; lastAPDUsize :"+lastAPDUsize+"; Total length: "+(nbAPDUMax*MAXLENGTH+lastAPDUsize)+"bytes");


				/* envoi des valeurs */
				System.out.println("==========Requete: Valeurs Variables==========");
				byte[] header3 = {CLA,WRITEFILETOCARD,P1_VAR,P2}; // requete de type "var" (contient nbAPDUMax et lastAPDUsize)
				byte[] optional3 = {(byte)0x02,(byte)nbAPDUMax,(byte)lastAPDUsize};
				byte[] command3 = new byte[(byte)header3.length + (byte)optional3.length];
				System.arraycopy(header3,(byte)0,command3,(byte)0,(byte)header3.length);
				System.arraycopy(optional3,(byte)0,command3,(byte)header3.length,(byte)optional3.length);
				CommandAPDU cmd3 = new CommandAPDU( command3);
				ResponseAPDU resp3 = this.sendAPDU( cmd3, DISPLAY );
				/* end */
				System.out.println("==========Fin Requete: Valeurs Variables==========");

			}

		}

		}catch(Exception e){
			System.out.println(e);
		}
	}


	void updateWritePIN() {
		
		System.out.println("Saisissez le NOUVEAU pin d'acces en ecriture:");
		String pin = readKeyboard();
		byte[] data = pin.getBytes();

		byte[] header = {CLA,UPDATEWRITEPIN, P1,P2};
		byte[] optional = new byte[(byte)1 + (byte)data.length]; // un byte pour Lc et data.lenght bytes


		optional[0] = (byte)data.length; // Lc = nb bytes de data
		System.arraycopy(data, (byte)0, optional, (byte)1, optional[0]);
		// copie du code pin vers le champ data (pas de Le)

		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		System.arraycopy(optional,(byte)0,command,(byte)header.length,(byte)optional.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void updateReadPIN() {

		System.out.println("Saisissez le NOUVEAU pin d'acces en lecture:");
		String pin = readKeyboard();
		byte[] data = pin.getBytes();

		byte[] header = {CLA,UPDATEREADPIN, P1,P2};
		byte[] optional = new byte[(byte)1 + (byte)data.length]; // un byte pour Lc et data.lenght bytes


		optional[0] = (byte)data.length; // Lc = nb bytes de data
		System.arraycopy(data, (byte)0, optional, (byte)1, optional[0]);
		// copie du code pin vers le champ data (pas de Le)

		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		System.arraycopy(optional,(byte)0,command,(byte)header.length,(byte)optional.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void displayPINSecurity() {

		byte[] header = {CLA,DISPLAYPINSECURITY, P1,P2};
		byte[] command = new byte[(byte)((byte)header.length +(byte)1)];
		System.arraycopy(header, (byte)0, command, (byte)0, (byte)header.length);
		command[4] = 0; // Le = 0 car attente de n bytes

		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );

		byte[] bytes = resp.getBytes();

	    String msg = "PIN security status: ";
		msg += new StringBuffer("").append((char)bytes[1]);
	    System.out.println(msg);
	}


	void desactivateActivatePINSecurity() {
		byte[] header = {CLA,DESACTIVATEACTIVATEPINSECURITY, P1,P2};
		byte[] command = new byte[(byte)header.length];
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void enterReadPIN() {
		System.out.println("Saisissez le pin d'acces en lecture:");
		String pin = readKeyboard();
		byte[] data = pin.getBytes();

		byte[] header = {CLA,ENTERREADPIN, P1,P2};
		byte[] optional = new byte[(byte)1 + (byte)data.length]; // un byte pour Lc et data.lenght bytes


		optional[0] = (byte)data.length; // Lc = nb bytes de data
		System.arraycopy(data, (byte)0, optional, (byte)1, optional[0]);
		// copie du code pin vers le champ data (pas de Le)

		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		System.arraycopy(optional,(byte)0,command,(byte)header.length,(byte)optional.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void enterWritePIN() {
		System.out.println("Saisissez le pin d'acces en ecriture:");
		String pin = readKeyboard();
		byte[] data = pin.getBytes();

		byte[] header = {CLA,ENTERWRITEPIN, P1,P2};
		byte[] optional = new byte[(byte)1 + (byte)data.length]; // un byte pour Lc et data.lenght bytes


		optional[0] = (byte)data.length; // Lc = nb bytes de data
		System.arraycopy(data, (byte)0, optional, (byte)1, optional[0]);
		// copie du code pin vers le champ data (pas de Le)

		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		System.arraycopy(header,(byte)0,command,(byte)0,(byte)header.length);
		System.arraycopy(optional,(byte)0,command,(byte)header.length,(byte)optional.length);
		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void readNameFromCard() {

		byte[] header = {CLA,READNAMEFROMCARD, P1,P2};
		byte[] command = new byte[(byte)((byte)header.length +(byte)1)];
		command[4] = 0; // Le = 0 car attente de data de taille inconnue
		System.arraycopy(header, (byte)0, command, (byte)0, (byte)header.length);


		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );

		byte[] bytes = resp.getBytes();
		

	    String msg = "";
	    for(int i=0; i<bytes.length-2;i++)
		    msg += new StringBuffer("").append((char)bytes[i]);
	    System.out.println(msg);
	}


	void writeNameToCard() {
		System.out.println("Saisissez le nom a inscrire sur la carte:");
		String name = readKeyboard();
		byte[] data = name.getBytes();

		byte[] header = {CLA,WRITENAMETOCARD, P1,P2};

		byte[] optional = new byte[(byte)1 + (byte)data.length]; // un byte pour Lc et data.lenght bytes
		optional[0] = (byte)data.length; // Lc = nb bytes de data

		System.arraycopy(data, (byte)0, optional, (byte)1, optional[0]);
		// copie de data vers "Optionnal part" (offset 1 car indice 0 = Lc), de taille Lc

		byte[] command = new byte[(byte)header.length + (byte)optional.length];
		// commande de taille Header + Optionnal part

		// copy de header vers command, de taille header
		System.arraycopy(header, (byte)0, command, (byte)0, (byte)header.length);
		//copy de optionalPart vers header (offset taille de header), de taille optional part
		System.arraycopy(optional, (byte)0, command,(byte)header.length, (byte)optional.length);
		

		CommandAPDU cmd = new CommandAPDU( command);
		ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
	}


	void exit() {
		loop = false;
	}


	void runAction( int choice ) {
		switch( choice ) {
			case 14: updateCardKey(); break;
			case 13: uncipherFileByCard(); break;
			case 12: cipherFileByCard(); break;
			case 11: cipherAndUncipherNameByCard(); break;
			case 10: readFileFromCard(); break;
			case 9: writeFileToCard(); break;
			case 8: updateWritePIN(); break;
			case 7: updateReadPIN(); break;
			case 6: displayPINSecurity(); break;
			case 5: desactivateActivatePINSecurity(); break;
			case 4: enterReadPIN(); break;
			case 3: enterWritePIN(); break;
			case 2: readNameFromCard(); break;
			case 1: writeNameToCard(); break;
			case 0: exit(); break;
			default: System.out.println( "unknown choice!" );
		}
	}


	String readKeyboard() {
		String result = null;

		try {
			BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
			result = input.readLine();
		} catch( Exception e ) {}

		return result;
	}


	int readMenuChoice() {
		int result = 0;

		try {
			String choice = readKeyboard();
			result = Integer.parseInt( choice );
		} catch( Exception e ) {}

		System.out.println( "" );

		return result;
	}


	void printMenu() {
		System.out.println( "" );
		System.out.println( "14: update the DES key within the card" );
		System.out.println( "13: uncipher a file by the card" );
		System.out.println( "12: cipher a file by the card" );
		System.out.println( "11: cipher and uncipher a name by the card" );
		System.out.println( "10: read a file from the card" );
		System.out.println( "9: write a file to the card" );
		System.out.println( "8: update WRITE_PIN" );
		System.out.println( "7: update READ_PIN" );
		System.out.println( "6: display PIN security status" );
		System.out.println( "5: desactivate/activate PIN security" );
		System.out.println( "4: enter READ_PIN" );
		System.out.println( "3: enter WRITE_PIN" );
		System.out.println( "2: read a name from the card" );
		System.out.println( "1: write a name to the card" );
		System.out.println( "0: exit" );
		System.out.print( "--> " );
	}


	void mainLoop() {
		while( loop ) {
			printMenu();
			int choice = readMenuChoice();
			runAction( choice );
		}
	}


	public static void main( String[] args ) throws InterruptedException {
		new TheClient();
	}


}
