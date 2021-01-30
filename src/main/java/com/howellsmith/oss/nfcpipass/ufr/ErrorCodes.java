package com.howellsmith.oss.nfcpipass.ufr;

public class ErrorCodes {

    public static final int DL_OK = 0x00;
    public static final int COMMUNICATION_ERROR = 0x01;
    public static final int CHECKSUM_ERROR = 0x02;
    public static final int READING_ERROR = 0x03;
    public static final int WRITING_ERROR = 0x04;
    public static final int BUFFER_OVERFLOW = 0x05;
    public static final int MAX_ADDRESS_EXCEEDED = 0x06;
    public static final int MAX_KEY_INDEX_EXCEEDED = 0x07;
    public static final int NO_CARD = 0x08;
    public static final int COMMAND_NOT_SUPPORTED = 0x09;
    public static final int FORBIDDEN_DIRECT_WRITE_IN_SECTOR_TRAILER = 0x0A;
    public static final int ADDRESSED_BLOCK_IS_NOT_SECTOR_TRAILER = 0x0B;
    public static final int WRONG_ADDRESS_MODE = 0x0C;
    public static final int WRONG_ACCESS_BITS_VALUES = 0x0D;
    public static final int AUTH_ERROR = 0x0E;
    public static final int PARAMETERS_ERROR = 0x0F;
    public static final int MAX_SIZE_EXCEEDED = 0x10;
    public static final int UNSUPPORTED_CARD_TYPE = 0x11;
    public static final int COMMUNICATION_BREAK = 0x50;
    public static final int NO_MEMORY_ERROR = 0x51;
    public static final int CAN_NOT_OPEN_READER = 0x52;
    public static final int READER_NOT_SUPPORTED = 0x53;
    public static final int READER_OPENING_ERROR = 0x54;
    public static final int READER_PORT_NOT_OPENED = 0x55;
    public static final int CANT_CLOSE_READER_PORT = 0x56;
    public static final int WRITE_VERIFICATION_ERROR = 0x70;
    public static final int BUFFER_SIZE_EXCEEDED = 0x71;
    public static final int VALUE_BLOCK_INVALID = 0x72;
    public static final int VALUE_BLOCK_ADDR_INVALID = 0x73;
    public static final int VALUE_BLOCK_MANIPULATION_ERROR = 0x74;
    public static final int WRONG_UI_MODE = 0x75;
    public static final int KEYS_LOCKED = 0x76;
    public static final int KEYS_UNLOCKED = 0x77;
    public static final int WRONG_PASSWORD = 0x78;
    public static final int CAN_NOT_LOCK_DEVICE = 0x79;
    public static final int CAN_NOT_UNLOCK_DEVICE = 0x7A;
    public static final int DEVICE_EEPROM_BUSY = 0x7B;
    public static final int RTC_SET_ERROR = 0x7C;
    public static final int ANTICOLLISION_DISABLED = 0x7D;
    public static final int NO_CARDS_ENUMERATED = 0x7E;
    public static final int CARD_ALREADY_SELECTED = 0x7F;
    public static final int FT_STATUS_ERROR_1 = 0xA0;
    public static final int FT_STATUS_ERROR_2 = 0xA1;
    public static final int FT_STATUS_ERROR_3 = 0xA2;
    public static final int FT_STATUS_ERROR_4 = 0xA3;
    public static final int FT_STATUS_ERROR_5 = 0xA4;
    public static final int FT_STATUS_ERROR_6 = 0xA5;
    public static final int FT_STATUS_ERROR_7 = 0xA6;
    public static final int FT_STATUS_ERROR_8 = 0xA7;
    public static final int FT_STATUS_ERROR_9 = 0xA8;

    private ErrorCodes(){}
}
