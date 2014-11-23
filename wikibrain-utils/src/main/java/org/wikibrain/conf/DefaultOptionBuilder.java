package org.wikibrain.conf;


/****************************************************************************
 *   (c) Copyright 2011 Zilliant Inc. All rights reserved.                  *
 * **************************************************************************
 *                                                                          *
 *  THIS MATERIAL IS PROVIDED "AS IS." ZILLIANT INC. DISCLAIMS ALL          *
 *  WARRANTIES OF ANY KIND WITH REGARD TO THIS MATERIAL, INCLUDING,         *
 *  BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,           *
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.                   *
 *                                                                          *
 *  Zilliant Inc. shall not be liable for errors contained herein           *
 *  or for incidental or consequential damages in connection with the       *
 *  furnishing, performance, or use of this material.                       *
 *                                                                          *
 *  Zilliant Inc. assumes no responsibility for the use or reliability      *
 *  of interconnected equipment that is not furnished by Zilliant Inc,      *
 *  or the use of Zilliant software with such equipment.                    *
 *                                                                          *
 *  This document or software contains trade secrets of Zilliant Inc. as    *
 *  well as proprietary information which is protected by copyright.        *
 *  All rights are reserved.  No part of this document or software may be   *
 *  photocopied, reproduced, modified or translated to another language     *
 *  prior written consent of Zilliant Inc.                                  *
 *                                                                          *
 *  The information contained herein has been prepared by Zilliant Inc.     *
 *  solely for use by Zilliant Inc., its employees, agents and customers.   *
 *  Dissemination of the information and/or concepts contained herein to    *
 *  other parties is prohibited without the prior written consent of        *
 *  Zilliant Inc..                                                          *
 *                                                                          *
 *  (c) Copyright 2011 by Zilliant.  All rights reserved.                   *
 *                                                                          *
 ****************************************************************************/

import org.apache.commons.cli.Option;

/**
 * DefaultOptionBuilder is a non-static version of OptionBuilder that truly follows the builder pattern.  We are
 * attempting to roll this version back into the apache CLI library.
 */
public final class DefaultOptionBuilder
{
    /**
     * long option
     */
    private String longopt;

    /**
     * option description
     */
    private String description;

    /**
     * argument name
     */
    private String argName;

    /**
     * is required?
     */
    private boolean required;

    /**
     * the number of arguments
     */
    private int numberOfArgs = Option.UNINITIALIZED;

    /**
     * option type
     */
    private Class type;

    /**
     * option can have an optional argument value
     */
    private boolean optionalArg;

    /**
     * value separator for argument value
     */
    private char valuesep;

    /**
     * private constructor to prevent instances being created
     */
    public DefaultOptionBuilder()
    {
        description = null;
        argName = null;
        longopt = null;
        type = String.class;
        required = false;
        numberOfArgs = Option.UNINITIALIZED;
        optionalArg = false;
        valuesep = (char) 0;
    }

    /**
     * The next Option created will have the following long option value.
     *
     * @param newLongopt the long option value
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withLongOpt(String newLongopt)
    {
        longopt = newLongopt;

        return this;
    }

    /**
     * The next Option created will require an argument value.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasArg()
    {
        numberOfArgs = 1;

        return this;
    }

    /**
     * The next Option created will require an argument value if
     * <code>hasArg</code> is true.
     *
     * @param hasArg if true then the Option has an argument value
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasArg(boolean hasArg)
    {
        numberOfArgs = hasArg ? 1 : Option.UNINITIALIZED;

        return this;
    }

    /**
     * The next Option created will have the specified argument value name.
     *
     * @param name the name for the argument value
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withArgName(String name)
    {
        argName = name;

        return this;
    }

    /**
     * The next Option created will be required.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder isRequired()
    {
        required = true;

        return this;
    }

    /**
     * The next Option created uses <code>sep</code> as a means to
     * separate argument values.
     *
     * <b>Example:</b>
     * <pre>
     * Option opt = OptionBuilder.withValueSeparator(':')
     *                           .create('D');
     *
     * CommandLine line = parser.parse(args);
     * String propertyName = opt.getValue(0);
     * String propertyValue = opt.getValue(1);
     * </pre>
     *
     * @param sep The value separator to be used for the argument values.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withValueSeparator(char sep)
    {
        valuesep = sep;

        return this;
    }

    /**
     * The next Option created uses '<code>=</code>' as a means to
     * separate argument values.
     *
     * <b>Example:</b>
     * <pre>
     * Option opt = OptionBuilder.withValueSeparator()
     *                           .create('D');
     *
     * CommandLine line = parser.parse(args);
     * String propertyName = opt.getValue(0);
     * String propertyValue = opt.getValue(1);
     * </pre>
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withValueSeparator()
    {
        valuesep = '=';

        return this;
    }

    /**
     * The next Option created will be required if <code>required</code>
     * is true.
     *
     * @param newRequired if true then the Option is required
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder isRequired(boolean newRequired)
    {
        required = newRequired;

        return this;
    }

    /**
     * The next Option created can have unlimited argument values.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasArgs()
    {
        numberOfArgs = Option.UNLIMITED_VALUES;

        return this;
    }

    /**
     * The next Option created can have <code>num</code> argument values.
     *
     * @param num the number of args that the option can have
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasArgs(int num)
    {
        numberOfArgs = num;

        return this;
    }

    /**
     * The next Option can have an optional argument.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasOptionalArg()
    {
        numberOfArgs = 1;
        optionalArg = true;

        return this;
    }

    /**
     * The next Option can have an unlimited number of optional arguments.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasOptionalArgs()
    {
        numberOfArgs = Option.UNLIMITED_VALUES;
        optionalArg = true;

        return this;
    }

    /**
     * The next Option can have the specified number of optional arguments.
     *
     * @param numArgs - the maximum number of optional arguments
     * the next Option created can have.
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder hasOptionalArgs(int numArgs)
    {
        numberOfArgs = numArgs;
        optionalArg = true;

        return this;
    }

    /**
     * The next Option created will have a value that will be an instance
     * of <code>type</code>.
     *
     * @param newType the type of the Options argument value
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withType(Class newType)
    {
        type = newType;

        return this;
    }

    /**
     * The next Option created will have the specified description
     *
     * @param newDescription a description of the Option's purpose
     *
     * @return the OptionBuilder instance
     */
    public DefaultOptionBuilder withDescription(String newDescription)
    {
        description = newDescription;

        return this;
    }

    /**
     * Create an Option using the current settings and with
     * the specified Option <code>char</code>.
     *
     * @param opt the character representation of the Option
     *
     * @return the Option instance
     *
     * @throws IllegalArgumentException if <code>opt</code> is not
     * a valid character.  See Option.
     */
    public Option create(char opt) throws IllegalArgumentException
    {
        return create(String.valueOf(opt));
    }

    /**
     * Create an Option using the current settings
     *
     * @return the Option instance
     *
     * @throws IllegalArgumentException if <code>longOpt</code> has not been set.
     */
    public Option create() throws IllegalArgumentException
    {
        if (longopt == null)
        {
            throw new IllegalArgumentException("must specify longopt");
        }

        return create(null);
    }

    /**
     * Create an Option using the current settings and with
     * the specified Option <code>char</code>.
     *
     * @param opt the <code>java.lang.String</code> representation
     * of the Option
     *
     * @return the Option instance
     *
     * @throws IllegalArgumentException if <code>opt</code> is not
     * a valid character.  See Option.
     */
    public Option create(String opt) throws IllegalArgumentException
    {
        Option option = new Option(opt, description);

        // set the option properties
        option.setLongOpt(longopt);
        option.setRequired(required);
        option.setOptionalArg(optionalArg);
        option.setArgs(numberOfArgs);
        option.setType(type);
        option.setValueSeparator(valuesep);
        option.setArgName(argName);

        // return the Option instance
        return option;
    }
}
