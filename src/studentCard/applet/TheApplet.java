package applet;


import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;



public class TheApplet extends Applet {


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

	static byte[] studentName = new byte[64];

	OwnerPIN pinRead;
	OwnerPIN pinWrite;

	final static byte  BINARY_WRITE = (byte) 0xD0;
	final static byte  BINARY_READ  = (byte) 0xB0;
	final static byte  SELECT       = (byte) 0xA4;
	final static byte  PIN_VERIFY   = (byte) 0x20;
	final static short SW_PIN_VERIFICATION_REQUIRED = (short) 0x6301;
	final static short SW_VERIFICATION_FAILED = (short)0x6300;
	final static short NVRSIZE      = (short)1024;
	static byte[] NVR               = new byte[NVRSIZE];

	boolean PINsecurity;
	static byte[] file = new byte[8192]; // 1Ko
	final static short MAXLENGTH = (short)255;
	static final byte P1_FILENAME 	 	= (byte)0x01;
	static final byte P1_BLOC 	 		= (byte)0x02;
	static final byte P1_VAR 	 		= (byte)0x03;
	static final byte P1_LASTBLOCK 	 		= (byte)0x04;
	




	private final static byte INS_DES_ECB_NOPAD_ENC           	= (byte)0x20;
    private final static byte INS_DES_ECB_NOPAD_DEC           	= (byte)0x21;

	static final byte[] theDESKey = 
		new byte[] { (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA, (byte)0xCA };
    // cipher instances
    private Cipher 
	    cDES_ECB_NOPAD_enc, cDES_ECB_NOPAD_dec;
    // key objects
    private Key 
	    secretDESKey, secretDES2Key, secretDES3Key;
    boolean 
	    pseudoRandom, secureRandom,
	    SHA1, MD5, RIPEMD160,
	    keyDES, DES_ECB_NOPAD, DES_CBC_NOPAD;




	protected TheApplet() {

		byte[] _pinRead_ = {(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30}; // PIN code "0000"
		byte[] _pinWrite_ = {(byte)0x31,(byte)0x31,(byte)0x31,(byte)0x31}; // PIN code "1111"
		pinRead = new OwnerPIN((byte)3,(byte)8);  				// 3 tries 8=Max Size
		pinRead.update(_pinRead_,(short)0,(byte)4); 				// from pincode, offset 0, length 4
		pinWrite = new OwnerPIN((byte)3,(byte)8);  				// 3 tries 8=Max Size
		pinWrite.update(_pinWrite_,(short)0,(byte)4); 				// from pincode, offset 0, length 4
		PINsecurity = true;	// init PINsecurity to true
	    initKeyDES(); 
	    initDES_ECB_NOPAD(); 
		
		this.register();
	}

    private void initKeyDES() {
	    try {
		    secretDESKey = KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);
		    ((DESKey)secretDESKey).setKey(theDESKey,(short)0);
		    keyDES = true;
	    } catch( Exception e ) {
		    keyDES = false;
	    }
    }


    private void initDES_ECB_NOPAD() {
	    if( keyDES ) try {
		    cDES_ECB_NOPAD_enc = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
		    cDES_ECB_NOPAD_dec = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
		    cDES_ECB_NOPAD_enc.init( secretDESKey, Cipher.MODE_ENCRYPT );
		    cDES_ECB_NOPAD_dec.init( secretDESKey, Cipher.MODE_DECRYPT );
		    DES_ECB_NOPAD = true;
	    } catch( Exception e ) {
		    DES_ECB_NOPAD = false;
	    }
    }

	private void cipherGeneric( APDU apdu, Cipher cipher) {
        byte[] buffer = apdu.getBuffer();
        
        /*Reception de la commande Client*/
        apdu.setIncomingAndReceive();
        byte Lc = buffer[4];
        /*Cipher*/
        cipher.doFinal( buffer, (short)5, (short)((short)Lc&(short)255), buffer, (short)0);
        /*Renvoi cipher vers Client*/
        apdu.setOutgoingAndSend((short)0, (short)((short)Lc&(short)255));

	}


	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		new TheApplet();
	} 


	public boolean select() {
		if ( pinRead.getTriesRemaining() == 0 || pinWrite.getTriesRemaining() == 0) // si 3 essais erronés successifs en lecture ou écriture, la carte est down
		return false;
		return true;
	} 


	public void deselect() {
		pinRead.reset();
		pinWrite.reset();
	}

	public void process(APDU apdu) throws ISOException {
		if( selectingApplet() == true )
			return;

		byte[] buffer = apdu.getBuffer();

		switch( buffer[1] ) 	{
			case UPDATECARDKEY: updateCardKey( apdu ); break;
			case UNCIPHERFILEBYCARD: uncipherFileByCard( apdu ); break;
			case CIPHERFILEBYCARD: cipherFileByCard( apdu ); break;
			case CIPHERANDUNCIPHERNAMEBYCARD: cipherAndUncipherNameByCard( apdu ); break;
			case READFILEFROMCARD: readFileFromCard( apdu ); break;
			case WRITEFILETOCARD: writeFileToCard( apdu ); break;
			case UPDATEWRITEPIN:
				if(!PINsecurity)
				{
					updateWritePIN( apdu );
				}
				else
				{
					if ( ! pinWrite.isValidated() )
					ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
					updateWritePIN( apdu );
				}
			break;
			case UPDATEREADPIN:
				if(!PINsecurity)
				{
					updateReadPIN( apdu );
				}
				else
				{
					if ( ! pinRead.isValidated() )
					ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
					updateReadPIN( apdu );
				}
			break;
			case DISPLAYPINSECURITY: displayPINSecurity( apdu ); break;
			case DESACTIVATEACTIVATEPINSECURITY: desactivateActivatePINSecurity( apdu ); break;
			case ENTERREADPIN: enterReadPIN( apdu ); break;
			case ENTERWRITEPIN: enterWritePIN( apdu ); break;
			case READNAMEFROMCARD:
				if(!PINsecurity)
				{
					readNameFromCard( apdu );
				}
				else
				{
					if ( ! pinRead.isValidated() )
					ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
					readNameFromCard( apdu );
				}
			break;
			case WRITENAMETOCARD: 
				if(!PINsecurity)
				{
					writeNameToCard( apdu );
				}
				else
				{
					if ( ! pinWrite.isValidated() )
					ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
					writeNameToCard( apdu );
				}
			break;
			default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}


	void updateCardKey( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		Util.arrayCopy(buffer, (byte)5, theDESKey, (byte)0,buffer[4]);
		initKeyDES(); 
	    initDES_ECB_NOPAD(); 
		
		/* Just to proove that DES key was succesfully updated ! */
		Util.arrayCopy(theDESKey, (byte)(0), buffer, (byte)0, (byte)8);
		apdu.setOutgoingAndSend((short)0, (short)8);
	}


	void uncipherFileByCard( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();
		cipherGeneric( apdu, cDES_ECB_NOPAD_dec);
	}


	void cipherFileByCard( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();
		cipherGeneric( apdu, cDES_ECB_NOPAD_enc);
	}


	void cipherAndUncipherNameByCard( APDU apdu ) {

		byte[] buffer = apdu.getBuffer();  
		
		switch(buffer[2]){
					case INS_DES_ECB_NOPAD_ENC: 
					if( DES_ECB_NOPAD )
					cipherGeneric( apdu, cDES_ECB_NOPAD_enc);
					break;
					case INS_DES_ECB_NOPAD_DEC: 
					if( DES_ECB_NOPAD ) 
					cipherGeneric( apdu, cDES_ECB_NOPAD_dec); 
					break;
					default:
				}
	}


	void readFileFromCard( APDU apdu ) {


		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		
		switch(buffer[2]){
			case P1_FILENAME:
				/* envoi filename */
				Util.arrayCopy(file, (byte)1, buffer, (byte)0, file[0]);
				apdu.setOutgoingAndSend((short)0, file[0]);
				/* end */
			break;
			case P1_BLOC:

					/* envoi d'un bloc */
					short offset = (short)((((byte)1 + file[0] + (byte)2) + (buffer[3] * (short)MAXLENGTH)));
					buffer = apdu.getBuffer();
					Util.arrayCopy(file, offset, buffer, (byte)0, (short)MAXLENGTH);
					apdu.setOutgoingAndSend((short)0, (short)MAXLENGTH);
					/* end */
			break;
			case P1_LASTBLOCK:

					
					/* envoi du dernier bloc */
					byte nbAPDUMax = file[(byte)(file[0]+(byte)1)];
					byte lastAPDUsize = file[(byte)(file[0]+(byte)2)];

					short offset_last = (short)((((byte)1 + (byte)file[0]) + (byte)2) + ((byte)(nbAPDUMax) * (short)MAXLENGTH));
					
					buffer = apdu.getBuffer();
					Util.arrayCopy(file, offset_last, buffer, (byte)0, (short)(lastAPDUsize&(short)255));
					apdu.setOutgoingAndSend((short)0, (short)(lastAPDUsize&(short)255));
					/* end */			
			break;
			case P1_VAR:
				/* envoi parametre nbAPDUMax et lastAPDUsize */
				Util.arrayCopy(file, (byte)(file[0]+(byte)1), buffer, (byte)0, (byte)2);
				apdu.setOutgoingAndSend((short)0, (byte)2);
				/* end */
			break;
			default:
		}
	}


	void writeFileToCard( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		
		switch(buffer[2]){
			case P1_FILENAME:
			Util.arrayCopy(buffer, (byte)4, file, (byte)0, (byte)(buffer[4]+(byte)1));
			break;
			case P1_BLOC:
			short offset = (short)((((byte)1 + file[0] + (byte)2) + (buffer[3] * (short)MAXLENGTH)));

			Util.arrayCopy(buffer, (byte)5, file, offset, (short)(buffer[4]&(short)255));
			break;
			case P1_VAR:
			Util.arrayCopy(buffer, (byte)5, file, (byte)((byte)1 + file[0]),(short)(buffer[4]&(short)255));
			break;
			default:
		}
	}


	void updateWritePIN( APDU apdu ) {

		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		pinWrite.update(buffer, (short)5, (byte)buffer[4]); // update pinWrite avec newpin, de size Lc
	}


	void updateReadPIN( APDU apdu ) { 

		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		byte[] newpin = new byte[buffer[4]];
		pinRead.update(buffer, (short)5, (byte)buffer[4]); // update pinRead avec newpin, de size Lc

	}


	void displayPINSecurity( APDU apdu ){

		byte[] buffer = apdu.getBuffer();

		if(PINsecurity)
		{
		buffer[0] = (byte)0x00; // pour avoir 2 bytes
		buffer[1] = (byte)0x31; // envoit 1 si Pinsecurity enable
		}
		else
		{
		buffer[0] = (byte)0x00; // pour avoir 2 bytes
		buffer[1] = (byte)0x30; // envoit 0 si Pinsecurity disable
		}
		
		apdu.setOutgoingAndSend((short)0, (byte)2);
	}


	void desactivateActivatePINSecurity( APDU apdu ) {
		if(PINsecurity) 
		{
			PINsecurity = false;
		}
		else
		{
			PINsecurity = true;
		}
	}


	void enterReadPIN( APDU apdu ) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		if( !pinRead.check( buffer, (byte)5, buffer[4] ) ) 
			ISOException.throwIt( SW_VERIFICATION_FAILED );
	}


	void enterWritePIN( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		if( !pinWrite.check( buffer, (byte)5, buffer[4] ) ) 
			ISOException.throwIt( SW_VERIFICATION_FAILED );
	}


	void readNameFromCard( APDU apdu ) {

		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(studentName, (byte)1, buffer, (byte)0, studentName[0]);
		// copy de studentname (avec offset 1 pour skip Lc) de taille Lc
		apdu.setOutgoingAndSend((short)0, studentName[0]);
	}


	void writeNameToCard( APDU apdu ) {
		byte[] buffer = apdu.getBuffer();  
		apdu.setIncomingAndReceive();
		Util.arrayCopy(buffer, (byte)4, studentName, (byte)0, (byte)(buffer[4]+(byte)1));
		// buffer = requeteClient , offset 4 = Lc
		// NB: on ecrit dans studentname: Lc + data (taille Lc+1, pour pouvoir contenir Lc et la taille de data)
	}


}
