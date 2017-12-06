/**
 * Test Payment Example
 *
 * Cardstream Java SDK
 * Copyright (C) 2017  Cardstream
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
import com.cardstream.*;

public class PaymentExample {

	public static void main(String args[]) {

		// Initialise the Payment Gateway
		Transaction payment = new Transaction("https://gateway.cardstream.com/direct/", "100001", "Circle4Take40Idea");


		// Merchant data
		//payment.addMerchantData("key", "value");

		try {

			payment.setAmount(100);
			payment.setType(Transaction.TYPE.ECOM);
			payment.setAction(Transaction.ACTION.SALE);
			payment.setCountryCode("GB");
			payment.setCurrencyCode(826);

			payment.setOrderRef("Test Payment");

			//card fields
			// payment.setXref("");
			payment.setCardNumber("4929421234600821");
			payment.setCardCVV("356");
			//  payment.setCardStartYear("07");
			//  payment.setCardStartMonth("04");
			payment.setCardExpiryMonth(12);
			payment.setCardExpiryYear(17);
			// payment.setCardIssueNumber("1");

			// Customer details
			payment.setCustomerName("John Smith");
			payment.setCustomerAddress("Flat 6, Primrose Rise, 347 Lavender Road, Northampton");

			payment.setCustomerPostcode("NN17 8YG");
			payment.setCustomerEmail("john.smith@example.com");
			payment.setCustomerPhone("01234 567890");

			payment.setDiscountValue(20);

			// Authorise the payment
			payment.authorise();

			// Ensure the request was sent
			if (!payment.isHttpSuccess()) {
				System.out.println("Request failed");
				return;
			}

			// Check the authorisation response
			if (payment.getAuthResponseCode().equals("0")) {
				System.out.println("Card authorised successfully");
			} else if (payment.getAuthResponseCode().equals("2")) {
				System.out.println("Card areferred");
			} else if (payment.getAuthResponseCode().equals("4")) {
				System.out.println("Card decline - keep card");
			} else if (payment.getAuthResponseCode().equals("5")) {
				System.out.println("Card declined");
			} else if (payment.getAuthResponseCode().equals("30")) {
				System.out.println("Authorisation failed: " + payment.getAuthMessage());
			} else {
				System.out.println("Unknown Cardstream response: "+payment.getAuthResponseCode()+": " + payment.getAuthMessage());
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
