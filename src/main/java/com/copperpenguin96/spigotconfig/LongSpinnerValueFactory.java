package com.copperpenguin96.spigotconfig;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.SpinnerValueFactory;

public class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
    private final long _min;
    private final long _max;
    private final long _stepValue;
    private final ObjectProperty<Long> _value = new SimpleObjectProperty<>();

    /**
     * Allows for longs to be used with spinners.
     */
    public LongSpinnerValueFactory(long min, long max, long init, long step) {
        _min = min;
        _max = max;
        _stepValue = step;
        setValue(init);
    }

    public LongSpinnerValueFactory(long min, long max, long init) {
        this(min, max, init, 1);
    }

    @Override
    public void decrement(int steps) {
        long current = getValue();
        long newValue = current - (steps * _stepValue);
        setValue(Math.max(_min, newValue));
    }

    @Override
    public void increment(int steps) {
        long currentValue = getValue();
        long newValue = currentValue + (steps * _stepValue);
        setValue(Math.min(_max, newValue));
    }

}
