package com.arturagapov.speechmaster;

import java.io.Serializable;

/**
 * Created by Artur Agapov on 19.11.2016.
 */
public class Data implements Serializable {
    private boolean premium = false;

    public Data(boolean premium) {
        this.premium = premium;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public static Data userData = new Data(false);
}
