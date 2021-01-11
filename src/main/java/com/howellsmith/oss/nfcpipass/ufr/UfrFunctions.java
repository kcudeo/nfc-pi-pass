package com.howellsmith.oss.nfcpipass.ufr;

import com.sun.jna.Library;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Native Interface for D-Logic uFR Series NFC Devices
 */
public interface UfrFunctions extends Library {

    String UFR_Status2String(int status);

    String GetDllVersionStr();

    int GetDlogicCardType(ByteByReference card_type);

    int TagEmulationStart();

    int TagEmulationStop();

    int TagEmulationStartRam();

    int TagEmulationStopRam();

    int erase_last_ndef_record(byte message_nr);

    int erase_all_ndef_records(byte message_nr);

    int ndef_card_initialization();

    int write_ndef_record_mirroring(byte message_nr, ByteByReference tnf, byte[] type_record, ByteByReference type_length,
                                    byte[] id, ByteByReference id_length, byte[] payload, IntByReference payload_length,
                                    ByteByReference card_formated, int use_uid_ascii_mirror, int use_counter_ascii_mirror,
                                    int payload_mirroring_pos);

    int write_ndef_record(byte message_nr, ByteByReference tnf, byte[] type_record, ByteByReference type_length, byte[] id,
                          ByteByReference id_length, byte[] payload, IntByReference payload_length, ByteByReference card_formated);

    int WriteEmulationNdef(byte tnf, byte[] type_record, byte type_length, byte[] id, byte id_length,
                           byte[] payload, byte payload_length);

    int WriteEmulationNdefRam(byte tnf, byte[] type_record, byte type_length,
                              byte[] id, byte id_length, byte[] payload, int payload_length);

    int read_ndef_record(byte message_nr, byte record_nr, ByteByReference tnf, byte[] type_record,
                         ByteByReference type_length, byte[] id, ByteByReference id_length, byte[] payload,
                         IntByReference payload_length);

    int WriteNdefRecord_WiFi(byte ndef_storage, String ssid, byte auth_type, byte encryption_type, String password);

    int ReadNdefRecord_WiFi(byte[] ssid, byte[] auth_type, byte[] encryption_type, byte[] password);

    int WriteNdefRecord_Bluetooth(byte ndef_storage, String bt_mac_address);

    int ReadNdefRecord_Bluetooth(byte[] bt_mac_address);

    int WriteNdefRecord_SMS(byte ndef_storage, String phone_number, String message);

    int ReadNdefRecord_SMS(byte[] phone_number, byte[] message);

    int WriteNdefRecord_GeoLocation(byte ndef_storage, String latitude, String longitude);

    int ReadNdefRecord_GeoLocation(byte[] latitude, byte[] longitude);

    int WriteNdefRecord_NaviDestination(byte ndef_storage, String destination);

    int ReadNdefRecord_NaviDestination(byte[] destination);

    int WriteNdefRecord_Email(byte ndef_storage, String email_address, String subject, String message);

    int WriteNdefRecord_Address(byte ndef_storage, String address);

    int ReadNdefRecord_Address(byte[] address);

    int ReadNdefRecord_Email(byte[] email_address, byte[] subject, byte[] message);

    int WriteNdefRecord_AndroidApp(byte ndef_storage, String package_name);

    int ReadNdefRecord_AndroidApp(byte[] package_name);

    int WriteNdefRecord_Text(byte ndef_storage, String text);

    int ReadNdefRecord_Text(byte[] text);

    int WriteNdefRecord_StreetView(byte ndef_storage, String latitude, String longitude);

    int ReadNdefRecord_StreetView(byte[] latitude, byte[] longitude);

    int WriteNdefRecord_Phone(byte ndef_storage, String phone_number);

    int ReadNdefRecord_Phone(byte[] phone_number);

    int WriteNdefRecord_Contact(byte ndef_storage, String name, String company, String address,
                                String phone, String email, String website);

    int ReadNdefRecord_Contact(byte[] vCard);

    int WriteNdefRecord_Bitcoin(byte ndef_storage, String bitcoin_address, String amount, String message);

    int ReadNdefRecord_Bitcoin(byte[] bitcoin_address, byte[] amount, byte[] message);

    int WriteNdefRecord_Skype(byte ndef_storage, String user_name, byte action);

    int ReadNdefRecord_Skype(byte[] user_name, byte[] action);

    int WriteNdefRecord_Whatsapp(byte ndef_storage, String message);

    int WriteNdefRecord_Viber(byte ndef_storage, String message);

    int ReadNdefRecord_Whatsapp(byte[] message);

    int ReadNdefRecord_Viber(byte[] message);

    int ReaderOpenEx(int reader_type, byte[] port_name, int port_interface, byte[] arg);

    int GetReaderType(IntByReference reader_type);

    int GetReaderSerialNumber(IntByReference reader_serial);

    int LinearFormatCard(byte[] key_a, byte block_access_bits, byte sector_access_bits, byte sector_trailers_byte9,
                         byte[] key_b, ShortByReference sector_formatted, byte auth_mode, byte key_index);

    int ReaderOpen();

    int ReaderClose();

    int GetReaderType(int[] iaReaderType);

    int GetCardId(ByteByReference bCardType, IntByReference iCardSerial);

    int GetCardIdEx(ByteByReference bCardType, byte[] baCardUID, ByteByReference bUidSize);

    int ReaderUISignal(int iLightMode, int iSoundMode);

    int LinearRead(byte[] baReadData, int iLinearAddress, int iDataLength, ShortByReference shBytesRet, byte bAuthMode, byte bKeyIndex);

    int LinearWrite(byte[] baWriteData, int iLinearAddress, int iDataLength, ShortByReference shBytesWritten, byte bAuthMode, byte bKeyIndex);

    int BlockWrite(byte[] saBlockWrite, int iBlockAddress, byte bAuthMode, byte bKeyIndex);

    int GetReaderSerialNumber(int[] reader_serial);

    int GetCardId(byte[] card_id, int[] card_serial);

    int ReaderSoftRestart();

    int ReaderReset();

    int ReadUserData(byte[] read_user_data);

    int WriteUserData(byte[] write_user_data);

    int ReaderKeyWrite(byte[] reader_key, byte key_index);

    int LinearRead(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_ret, byte auth_mode, int key_index);

    int LinearRead_AKM1(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_ret, byte auth_mode);

    int LinearRead_AKM2(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_ret, byte auth_mode);

    int LinearRead_PK(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_ret, byte auth_mode, byte[] pk_key);

    int LinearWrite(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_written, byte auth_mode, int key_index);

    int LinearWrite_AKM1(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_written, byte auth_mode);

    int LinearWrite_AKM2(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_written, byte auth_mode);

    int LinearWrite_PK(byte[] aucdata, int linear_address, int data_length, ShortByReference bytes_written, byte auth_mode, byte[] pk_key);

    int BlockRead(byte[] block_data, byte block_address, byte auth_mode, byte key_index);

    int BlockRead_AKM1(byte[] block_data, byte block_address, byte auth_mode);

    int BlockRead_AKM2(byte[] block_data, byte block_address, byte auth_mode);

    int BlockRead_PK(byte[] block_data, byte block_address, byte auth_mode, byte[] pk_key);

    int BlockWrite(byte[] block_data, byte block_address, byte auth_mode, byte key_index);

    int BlockWrite_AKM1(byte[] block_data, byte block_address, byte auth_mode);

    int BlockWrite_AKM2(byte[] block_data, byte block_address, byte auth_mode);

    int BlockWrite_PK(byte[] block_data, byte block_address, byte auth_mode, byte[] pk_key);

    int BlockInSectorRead(byte[] block_data, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int BlockInSectorRead_AKM1(byte[] block_data, byte sector_address, byte block_address, byte auth_mode);

    int BlockInSectorRead_AKM2(byte[] block_data, byte sector_address, byte block_address, byte auth_mode);

    int BlockInSectorRead_PK(byte[] block_data, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int BlockInSectorWrite(byte[] block_data, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int BlockInSectorWrite_AKM1(byte[] block_data, byte sector_address, byte block_address, byte auth_mode);

    int BlockInSectorWrite_AKM2(byte[] block_data, byte sector_address, byte block_address, byte auth_mode);

    int BlockInSectorWrite_PK(byte[] block_data, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockRead(IntByReference value_read, ShortByReference value_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockRead_AKM1(IntByReference value_read, ShortByReference value_address, byte block_address, byte auth_mode);

    int ValueBlockRead_AKM2(IntByReference value_read, ShortByReference value_address, byte block_address, byte auth_mode);

    int ValueBlockRead_PK(IntByReference value_read, ShortByReference value_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockWrite(int value_read, short value_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockWrite_AKM1(int value_read, short value_address, byte block_address, byte auth_mode);

    int ValueBlockWrite_AKM2(int value_read, short value_address, byte block_address, byte auth_mode);

    int ValueBlockWrite_PK(int value_read, short value_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockIncrement(int incr_value, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockIncrement_AKM1(int incr_value, byte block_address, byte auth_mode);

    int ValueBlockIncrement_AKM2(int incr_value, byte block_address, byte auth_mode);

    int ValueBlockIncrement_PK(int incr_value, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockDecrement(int incr_value, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockDecrement_AKM1(int incr_value, byte block_address, byte auth_mode);

    int ValueBlockDecrement_AKM2(int incr_value, byte block_address, byte auth_mode);

    int ValueBlockDecrement_PK(int incr_value, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockInSectorRead(IntByReference read_data, ShortByReference value_address, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockInSectorRead_AKM1(IntByReference read_data, ShortByReference value_address, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorRead_AKM2(IntByReference read_data, ShortByReference value_address, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorRead_PK(IntByReference read_data, ShortByReference value_address, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockInSectorWrite(int read_data, short value_address, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockInSectorWrite_AKM1(int read_data, short value_address, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorWrite_AKM2(int read_data, short value_address, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorWrite_PK(int read_data, short value_address, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockInSectorIncrement(int read_data, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockInSectorIncrement_AKM1(int read_data, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorIncrement_AKM2(int read_data, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorIncrement_PK(int read_data, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int ValueBlockInSectorDecrement(int read_data, byte sector_address, byte block_address, byte auth_mode, byte key_index);

    int ValueBlockInSectorDecrement_AKM1(int read_data, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorDecrement_AKM2(int read_data, byte sector_address, byte block_address, byte auth_mode);

    int ValueBlockInSectorDecrement_PK(int read_data, byte sector_address, byte block_address, byte auth_mode, byte[] pk_key);

    int LinearFormatCard(byte[] key_a, byte block_access_bits, byte sector_access_bits, byte sector_trailers_byte9,
                         byte[] key_b, ByteByReference sector_formatted, byte auth_mode, byte key_index);

    int LinearFormatCard_AKM1(byte[] key_a, byte block_access_bits, byte sector_access_bits, byte sector_trailers_byte9,
                              byte[] key_b, ByteByReference sector_formatted, byte auth_mode);

    int LinearFormatCard_AKM2(byte[] key_a, byte block_access_bits, byte sector_access_bits, byte sector_trailers_byte9,
                              byte[] key_b, ByteByReference sector_formatted, byte auth_mode);

    int LinearFormatCard_PK(byte[] key_a, byte block_access_bits, byte sector_access_bits, byte sector_trailers_byte9,
                            byte[] key_b, ByteByReference sector_formatted, byte auth_mode, byte[] pk_key);

    int SectorTrailerWrite(byte addressing_mode, byte address, byte[] key_a, byte block_access_bits0,
                           byte block_access_bits1, byte block_access_bits2, byte sector_trailer_access_bits,
                           byte sector_trailer_byte9, byte[] key_b, byte auth_mode, byte key_index);

    int SectorTrailerWrite_AKM1(byte addressing_mode, byte address, byte[] key_a, byte block_access_bits0,
                                byte block_access_bits1, byte block_access_bits2, byte sector_trailer_access_bits,
                                byte sector_trailer_byte9, byte[] key_b, byte auth_mode);

    int SectorTrailerWrite_AKM2(byte addressing_mode, byte address, byte[] key_a, byte block_access_bits0,
                                byte block_access_bits1, byte block_access_bits2, byte sector_trailer_access_bits,
                                byte sector_trailer_byte9, byte[] key_b, byte auth_mode);

    int SectorTrailerWrite_PK(byte addressing_mode, byte address, byte[] key_a, byte block_access_bits0,
                              byte block_access_bits1, byte block_access_bits2, byte sector_trailer_access_bits,
                              byte sector_trailer_byte9, byte[] key_b, byte auth_mode, byte[] pk_key);

}
