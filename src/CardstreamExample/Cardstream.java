package CardstreamExample;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;

public class Cardstream {

    private final String GATEWAY_URL = "https://gateway.cardstream.com/direct/";
    private String reqString = "";
    private String resString = "";
    private Boolean httpSuccess = false;
    private final HashMap<String, String> resCol = new HashMap<>();
    private final TreeMap<String, String> formData = new TreeMap<>();

    private String merchantId = "";
    private String merchantPassword = "";
    private int amount;
    private int countryCode;
    private int currencyCode;
    private String uniqueIdentifier = "";
    private String cardNumber = "";
    private String customerName = "";
    private String cardIssueNumber = "";
    private String cardExpiryMM = "";
    private String cardExpiryYY = "";
    private String callBackUrl = "";
    private int type;
    private String action;
    private String orderRef;
    private String xref;
    private String cardCVV;
    private String cardStartYear;
    private String customerAddress;
    private String customerPostcode;
    private String customerEmail;
    private String customerPhone;
    private String taxDiscountDescription;
    private String cardStartMonth;
    private String preSharedKey;

    public Boolean Authorise() throws Exception {
        try {
            // Build the form, ensuring the necessary parameters have been defined
            BuildForm();

            // Send the form
            if (!SendForm()) {
                return false;
            }

            // Parse the response
            ParseResponse();

            return true;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void BuildForm() throws Exception {

        //  Check the merchant ID
        if (merchantId == null || merchantId.length() == 0) {
            throw new Exception("Merchant ID not set");
        }

        //  Check the merchant password 
        if (merchantPassword == null || merchantPassword.length() == 0) {
            /**
             * this should be uncommented for production code
             */    
            // throw new Exception("Merchant password not set");
        }

        // Check the amount
        if (amount == 0) {
            throw new Exception("Amount not set");
        } else if (amount < 10) {
            throw new Exception("Amount must be not be lower than 10");
        }

        // Check the country code
        if (countryCode == 0) {
            throw new Exception("Country code not set");
        }

        // Check the currency code
        if (currencyCode == 0) {
            throw new Exception("Currency code not set");
        }

        // Check the unique identifier
        if (uniqueIdentifier == null || uniqueIdentifier.length() == 0) {
            throw new Exception("Unique identifier not set");
        }

        // Check the card number
        if (cardNumber == null || cardNumber.length() == 0) {
            throw new Exception("Card number not set");
        } else if (cardNumber.length() < 16) {
            throw new Exception("Card number must be not be lower than 16 digits");
        }

        // Check the card name
        if (customerName == null || customerName.length() == 0) {
            throw new Exception("Card name not set");
        }

        // Check the card expiry month
        if (cardExpiryMM == null || cardExpiryMM.length() == 0) {
            throw new Exception("Card expiry month not set");
        } else if (cardExpiryMM.length() != 2) {
            throw new Exception("Card expiry month must be 2 characters");
        }

        // Check the card expiry year
        if (cardExpiryYY == null || cardExpiryYY.length() == 0) {
            throw new Exception("Card expiry year not set");
        } else if (cardExpiryYY.length() != 2) {
            throw new Exception("Card expiry year must be 2 characters");
        }

        // Check for a callback URL
        if (callBackUrl == null) {
            formData.put("callbackURL", "disable");
        }

        Set keyset = formData.keySet();
        Iterator iterator = keyset.iterator();

        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) {
                reqString += "&";
            }
            first = false;

            String propertyKey = (String) iterator.next();
            reqString += URLEncoder.encode(propertyKey, "ISO-8859-1") + "=" + URLEncoder.encode(formData.get(propertyKey), "ISO-8859-1");
        }
        reqString += "&signature=" + hashFormData(reqString + this.preSharedKey);
    }

    private String hashFormData(String Data) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-512");

            md.update(Data.getBytes());

            byte[] mb = md.digest();
            String out = "";
            for (int i = 0; i < mb.length; i++) {
                byte temp = mb[i];
                String s = Integer.toHexString(new Byte(temp));
                while (s.length() < 2) {
                    s = "0" + s;
                }
                s = s.substring(s.length() - 2);
                out += s;
            }
            //System.out.println(out.length());
            //System.out.println("CRYPTO: " + out);

            return out;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        return null;

    }

    private boolean SendForm() {
        String line;

        try {

            // Create the request
            URL reqUrl = new URL(GATEWAY_URL);
            HttpURLConnection reqConn = (HttpURLConnection) reqUrl.openConnection();
            reqConn.setDoInput(true);
            reqConn.setDoOutput(true);
            reqConn.setUseCaches(false);
            reqConn.setRequestMethod("POST");
            reqConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            reqConn.setRequestProperty("Connection", "Close");
            reqConn.setRequestProperty("charset", "utf-8");
            reqConn.setRequestProperty("Content-Length", "" + Integer.toString(reqString.getBytes().length));
            try (DataOutputStream reqStream = new DataOutputStream(reqConn.getOutputStream())) {
                reqStream.writeBytes(reqString);
                reqStream.flush();
            }
            try (BufferedReader resBuf = new BufferedReader(new InputStreamReader(reqConn.getInputStream()))) {
                while (true) {
                    line = resBuf.readLine();

                    if (line == null) {
                        break;
                    }

                    resString += line;
                }
            }

            httpSuccess = true;

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    private void ParseResponse() {
        if (resString.length() > 0) {
            for (String kvPairStr : resString.split("&")) {
                String[] kvPair = kvPairStr.split("=");

                if (kvPair.length == 2) {
                    resCol.put(kvPair[0], kvPair[1]);
                }
            }
        }
    }

    public void setMerchantId(String merchantId) {
        this.formData.put("merchantID", merchantId);
        this.merchantId = merchantId;
    }

    public void setPreSharedKey(String preSharedKey) {
     
        this.preSharedKey = preSharedKey;
    }

    public void setAmount(int amount) {
        this.formData.put("amount", "" + amount);
        this.amount = amount;
    }

    public void setCountryCode(int countryCode) {
        this.formData.put("countryCode", "" + countryCode);
        this.countryCode = countryCode;
    }

    public void setCurrencyCode(int currencyCode) {
        this.formData.put("currencyCode", "" + currencyCode);
        this.currencyCode = currencyCode;
    }

    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.formData.put("uniqueIdentifier", uniqueIdentifier);
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public void setCardNumber(String cardNumber) {
        this.formData.put("cardNumber", cardNumber);
        this.cardNumber = cardNumber;
    }

    public void setCustomerName(String customerName) {
        this.formData.put("customerName", customerName);
        this.customerName = customerName;
    }

    public void setCardIssueNumber(String cardIssueNumber) {
        this.formData.put("cardIssueNumber", cardIssueNumber);
        this.cardIssueNumber = cardIssueNumber;
    }

    public void setCardExpiryMM(String cardExpiryMM) {
        this.formData.put("cardExpiryMonth", cardExpiryMM);
        this.cardExpiryMM = cardExpiryMM;
    }

    public void setCardExpiryYY(String cardExpiryYY) {
        this.formData.put("cardExpiryYear", cardExpiryYY);
        this.cardExpiryYY = cardExpiryYY;
    }

    public void setCallBackUrl(String callBackUrl) {
        this.callBackUrl = callBackUrl;
    }

    public void setType(int type) {
        this.formData.put("type", type + "");
        this.type = type;
    }

    public void setAction(String action) {
        this.formData.put("action", action);
        this.action = action;
    }

    public void setOrderRef(String ref) {
        this.formData.put("orderRef", ref);
        this.orderRef = ref;
    }

    public void setXref(String xref) {
        this.formData.put("xref", xref);
        this.xref = xref;
    }

    public void setCardCVV(String cardCVV) {
        this.formData.put("cardCVV", cardCVV);
        this.cardCVV = cardCVV;
    }

    public void setCardStartYear(String cardStartYear) {
        this.formData.put("cardStartYear", cardStartYear);
        this.cardStartYear = cardStartYear;
    }

    public void setCardStartMonth(String cardStartMonth) {
        this.formData.put("cardStartMonth", cardStartMonth);
        this.cardStartMonth = cardStartMonth;
    }

    public void setCustomerAddress(String customerAddress) {
        this.formData.put("customerAddress", customerAddress);
        this.customerAddress = customerAddress;
    }

    public void setCustomerPostcode(String customerPostcode) {
        this.formData.put("customerPostcode", customerPostcode);
        this.customerPostcode = customerPostcode;
    }

    public void setCustomerEmail(String customerEmail) {
        this.formData.put("customerEmail", customerEmail);
        this.customerEmail = customerEmail;
    }

    public void setCustomerPhone(String customerPhone) {
        this.formData.put("customerPhone", customerPhone);
        this.customerPhone = customerPhone;
    }

    public void addItem(String description, int quantity, int value) {
        //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setTaxValue(int i) {
        formData.remove("discountValue");
        formData.put("taxValue", i + "");
    }

    public void setDiscountValue(int i) {
        formData.remove("taxValue");
        formData.put("discountValue", i + "");
    }

    public void setTaxDiscountDescription(String description) {
        this.formData.put("taxDiscountDescription", description);
        this.taxDiscountDescription = description;
    }

    public void addMerchantData(String key, String value) {
        this.formData.put("merchantData[" + key + "]", value);
    }

    public Boolean isHttpSuccess() {
        return this.httpSuccess;
    }

    public String getAuthResponseCode() {
        return this.resCol.get("responseCode");
    }

    public String getAuthMessage() {
        return this.resCol.get("responseMessage");
    }

    public String getAuthCrossReference() {
        return this.resCol.get("VPCrossReference");
    }

    public String getAuthOrderDescription() {
        return this.resCol.get("VPOrderDesc");
    }

    public String getAuthUniqueIndentifier() {
        return this.resCol.get("VPTransactionUnique");
    }

}
