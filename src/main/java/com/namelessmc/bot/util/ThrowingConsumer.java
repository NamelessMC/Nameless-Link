package com.namelessmc.bot.util;

@FunctionalInterface
public interface ThrowingConsumer<ParameterType, ThrowableType extends Throwable> {

	void accept(ParameterType arg) throws ThrowableType;

}
