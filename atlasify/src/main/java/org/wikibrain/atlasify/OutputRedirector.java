package org.wikibrain.atlasify;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Created by Josh on 1/20/15.
 */

/* Sends all commands to two PrintStreams allowing print commands
 * to be printed to the console and sent to Atlasify (God Mode)
 */

public class OutputRedirector extends PrintStream {
    private PrintStream stream;
    public OutputRedirector(OutputStream file, PrintStream second) {
        super(file);
        stream = second;
    }

    @Override
    public void flush() {
        stream.flush();
        super.flush();
    }

    @Override
    public void print(boolean b) {
        stream.print(b);
        super.print(b);
    }

    @Override
    public void print(char c) {
        stream.print(c);
        super.print(c);
    }

    @Override
    public void print(double d) {
        stream.print(d);
        super.print(d);
    }

    @Override
    public void print(float f) {
        stream.print(f);
        super.print(f);
    }

    @Override
    public void print(int i) {
        stream.print(i);
        super.print(i);
    }

    @Override
    public void print(long l) {
        stream.print(l);
        super.print(l);
    }

    @Override
    public void print(Object obj) {
        stream.print(obj);
        super.print(obj);
    }

    @Override
    public void print(char[] s) {
        stream.print(s);
        super.print(s);
    }

    @Override
    public void print(String s) {
        stream.print(s);
        super.print(s);
    }

    @Override
    public void close() {
        stream.close();
        super.close();
    }

    @Override
    public void println() {
        stream.println();
        super.println();
    }

    @Override
    public void println(boolean x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(char x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(int x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(long x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(float x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(double x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(char[] x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(String x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public void println(Object x) {
        stream.println(x);
        super.println(x);
    }

    @Override
    public boolean checkError() {
        stream.checkError();
        return super.checkError();
    }

    @Override
    public PrintStream append(char c) {
        stream.append(c);
        return super.append(c);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        stream.append(csq);
        return super.append(csq);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        stream.append(csq, start, end);
        return super.append(csq, start, end);
    }

    @Override
    public PrintStream format(String format, Object... args) {
        stream.format(format, args);
        return super.format(format, args);
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        stream.format(l, format, args);
        return super.format(l, format, args);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        stream.printf(format, args);
        return super.printf(format, args);
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        stream.printf(l, format, args);
        return super.printf(l, format, args);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream.print(b);
        super.write(b);
    }

    @Override
    public void write(int b) {
        stream.write(b);
        super.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        stream.write(buf, off, len);
        super.write(buf, off, len);
    }
}
