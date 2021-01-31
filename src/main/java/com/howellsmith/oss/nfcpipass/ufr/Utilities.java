package com.howellsmith.oss.nfcpipass.ufr;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Supplier;

import static com.sun.jna.Platform.is64Bit;
import static com.sun.jna.Platform.isARM;
import static com.sun.jna.Platform.isIntel;
import static com.sun.jna.Platform.isLinux;
import static com.sun.jna.Platform.isMac;
import static com.sun.jna.Platform.isPPC;
import static com.sun.jna.Platform.isWindows;

/**
 * uFR Utility class
 */
@Slf4j
@Component
public class Utilities {

    private static final String LIB_NAME_BASE = "uFCoder";
    private static final String LIB_PART_BLANK = "";
    private static final String LIB_POSTFIX_SEPARATOR = "-";
    private static final String LIB_PREFIX_LIB = "lib";
    private static final String LIB_PLATFORM_ARM = "armhf";
    private static final String LIB_PLATFORM_PPC = "ppc";
    private static final String LIB_PLATFORM_X86 = "x86";
    private static final String LIB_PLATFORM_64BIT = "_64";
    private static final String LIB_EXTENSION_MAC = "dylib";
    private static final String LIB_EXTENSION_LINUX = "so";
    private static final String LIB_EXTENSION_WINDOWS = "dll";
    private static final String LIB_PATH_MAC = "macos";
    private static final String LIB_PATH_LINUX = "linux";
    private static final String LIB_PATH_WINDOWS = "windows";

    private static final Supplier<String> GET_NATIVE_LIBRARY_FILE_NAME_PREFIX = () ->
            isMac() || isLinux() ? LIB_PREFIX_LIB : LIB_PART_BLANK;
    private static final Supplier<String> GET_NATIVE_LIBRARY_FILE_NAME_64BIT = () ->
            is64Bit() ? LIB_PLATFORM_64BIT : LIB_PART_BLANK;
    private static final Supplier<String> GET_NATIVE_LIBRARY_FILE_NAME_POSTFIX = () ->
            (isARM() ? LIB_PLATFORM_ARM
                    : isPPC() ? LIB_PLATFORM_PPC
                    : isIntel() ? isMac() ? LIB_PART_BLANK
                    : LIB_PLATFORM_X86
                    : LIB_PART_BLANK) + GET_NATIVE_LIBRARY_FILE_NAME_64BIT.get();
    private static final Supplier<String> GET_NATIVE_LIBRARY_FILE_EXTENSION = () ->
            isMac() ? LIB_EXTENSION_MAC
                    : isLinux() ? LIB_EXTENSION_LINUX
                    : isWindows() ? LIB_EXTENSION_WINDOWS
                    : LIB_PART_BLANK;
    public static final Supplier<String> NATIVE_LIBRARY_FILE_NAME = () ->
            String.format("%s%s%s.%s",
                    GET_NATIVE_LIBRARY_FILE_NAME_PREFIX.get(),
                    LIB_NAME_BASE,
                    Strings.isNotBlank(GET_NATIVE_LIBRARY_FILE_NAME_POSTFIX.get())
                            ? LIB_POSTFIX_SEPARATOR + GET_NATIVE_LIBRARY_FILE_NAME_POSTFIX.get()
                            : LIB_PART_BLANK,
                    GET_NATIVE_LIBRARY_FILE_EXTENSION.get());
    public static final Supplier<String> NATIVE_LIBRARY_ABSOLUTE_PATH = () ->
            String.join(File.separator, LIB_PREFIX_LIB,
                    isMac() ? LIB_PATH_MAC : isLinux() ? LIB_PATH_LINUX : isWindows() ? LIB_PATH_WINDOWS : LIB_PART_BLANK,
                    isARM() ? LIB_PLATFORM_ARM : LIB_PLATFORM_X86 + LIB_PLATFORM_64BIT,
                    NATIVE_LIBRARY_FILE_NAME.get());


}
