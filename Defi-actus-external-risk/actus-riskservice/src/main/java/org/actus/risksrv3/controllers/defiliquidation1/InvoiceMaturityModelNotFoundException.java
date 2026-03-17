package org.actus.risksrv3.controllers.defiliquidation1;

public class InvoiceMaturityModelNotFoundException extends RuntimeException {
    public InvoiceMaturityModelNotFoundException(String id) {
        super("InvoiceMaturityModel not found: " + id);
    }
}
