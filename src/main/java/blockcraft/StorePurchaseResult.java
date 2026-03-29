package blockcraft;


public record StorePurchaseResult(boolean success, String message) {
    public static StorePurchaseResult ok(String message) {
        return new StorePurchaseResult(true, message);
    }



    public static StorePurchaseResult fail(String message) {
        return new StorePurchaseResult(false, message);
    }
}

