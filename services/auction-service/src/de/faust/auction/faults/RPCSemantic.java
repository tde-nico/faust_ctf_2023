package de.faust.auction.faults;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RPCSemantic {
    RPCSemanticType value ();
}
