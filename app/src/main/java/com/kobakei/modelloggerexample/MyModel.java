package com.kobakei.modelloggerexample;

import com.kobakei.modellogger.LogField;
import com.kobakei.modellogger.Loggable;

/**
 * Created by keisuke on 15/11/19.
 */
@Loggable
public class MyModel {
    @LogField
    public String foo;
    @LogField
    public int bar;

    public MyModel(String foo, int bar) {
        this.foo = foo;
        this.bar = bar;
    }
}
