
import CardstreamExample.Cardstream;

public class example {

    public static void main(String args[]) {

        // Initialise the Cardstream object
        Cardstream cs = new Cardstream();

        //general fields
        cs.setMerchantId("100001");
        cs.setPreSharedKey("cCircle4Take40Idea");
        cs.setAmount(10);
        cs.setType(1);
        cs.setAction("PREAUTH");
        cs.setCountryCode(826);
        cs.setCurrencyCode(826);
        cs.setUniqueIdentifier("26482843");
        cs.setOrderRef("ref");

        //card fields
       // cs.setXref(""); 
        cs.setCardNumber("4929421234600821");
        cs.setCardCVV("356");
      //  cs.setCardStartYear("07");
      //  cs.setCardStartMonth("04");
        cs.setCardExpiryMM("09");
        cs.setCardExpiryYY("14");
       // cs.setCardIssueNumber("1");

        //verification
     //  cs.setCallBackUrl("callback");

        //customer details
        cs.setCustomerName("A Nother");
        cs.setCustomerAddress("Flat 6, Primrose Rise, 347 Lavender Road, Northampton");
        
        cs.setCustomerPostcode("NN17 8YG");
        cs.setCustomerEmail("a.nother@email.com");
        cs.setCustomerPhone("01234 567890");

        //american express and diners club
        cs.addItem("Description", 5, 25);
        cs.setDiscountValue(20);
        //or
        //cs.setTaxValue(20);
        
        //merchant data
        cs.addMerchantData("key", "value");

        try {

            // Authorise the payment
            cs.Authorise();

            // Ensure the request was sent
            if (!cs.isHttpSuccess()) {
                System.out.println("Request failed");
                return;
            }

            // Check the authorisation response
            if (cs.getAuthResponseCode().equals("0")) {
                System.out.println("Card authorised successfully");
            } else if (cs.getAuthResponseCode().equals("2")) {
                System.out.println("Card areferred");
            } else if (cs.getAuthResponseCode().equals("4")) {
                System.out.println("Card decline - keep card");
            } else if (cs.getAuthResponseCode().equals("5")) {
                System.out.println("Card declined");
            } else if (cs.getAuthResponseCode().equals("30")) {
                System.out.println("Authorisation failed: " + cs.getAuthMessage());
            } else {
                System.out.println("Unknown Cardstream response: "+cs.getAuthResponseCode()+": " + cs.getAuthMessage());
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
