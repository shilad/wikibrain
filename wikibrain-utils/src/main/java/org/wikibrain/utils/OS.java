/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package org.wikibrain.utils;

/**
 * This enum contains values for each type of OS.
 * <p>
 * To get the current OS:
 * <pre class="prettyprint">
 * OS myos = OS.{@link #get()};
 * System.out.println(myos);
 * </pre>
 * To check whether your OS is windows or unix;
 * <pre class="prettyprint">
 * OS myos = OS.get();
 * System.out.println("isWindows: "+myos.{@link #isWindows()});
 * System.out.println("isUnix: "+myos.{@link #isUnix()});
 * </pre>
 *
 * When your OS is not recognized, {@code OS.get()} returns OS.{@link #OTHER}
 * <p>
 * There is another usefult method which might be required rarely;
 * <pre class="prettyprint">
 * String osName = System.getProperty("os.name");
 * OS os = OS.{@link #get(String) get}(osName);
 * </pre>
 * This might be handy, if your app is distributed and and want to find out the os of remote JVM process
 *
 * @author Santhosh Kumar T
 */
public enum OS{
    WINDOWS_NT("Windows NT"),
    WINDOWS_95("Windows 95"),
    WINDOWS_98("Windows 98"),
    WINDOWS_2000("Windows 2000"),
    WINDOWS_VISTA("Windows Vista"),
    WINDOWS_7("Windows 7"),
    // add new windows versions here
    WINDOWS_OTHER("Windows"),

    SOLARIS("Solaris"),
    LINUX("Linux"),
    HP_UX("HP-UX"),
    IBM_AIX("AIX"),
    SGI_IRIX("Irix"),
    SUN_OS("SunOS"),
    COMPAQ_TRU64_UNIX("Digital UNIX"),
    MAC("Mac OS X", "Darwin"),
    FREEBSD("freebsd"),
    // add new unix versions here

    OS2("OS/2"),
    COMPAQ_OPEN_VMS("OpenVMS"),
    /** Unrecognized OS */
    OTHER("");

    private String names[];

    private OS(String... names){
        this.names = names;
    }

    /** @return true if this OS belongs to windows family */
    public boolean isWindows(){
        return ordinal()<=WINDOWS_OTHER.ordinal();
    }
    
    /** @return true if this OS belongs to *nix family */
    public boolean isUnix(){
        return ordinal()>WINDOWS_OTHER.ordinal() && ordinal()<OS2.ordinal();
    }

    /*-------------------------------------------------[ Static Methods ]---------------------------------------------------*/

    /**
     * @param osName name of OS as returned by <code>System.getProperty("os.name")</code>
     * 
     * @return OS for the specified {@code osName}
     */
    public static OS get(String osName){
        osName = osName.toLowerCase();
        for(OS os: values()){
            for(String name: os.names){
                if(osName.contains(name.toLowerCase()))
                    return os;
            }
        }
        throw new IllegalStateException();
    }

    private static OS current;
    /** @return OS on which this JVM is running */
    public static OS get(){
        if(current==null)
            current = get(System.getProperty("os.name"));
        return current;
    }
}
