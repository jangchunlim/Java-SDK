/**
 * Be able to process payments through Cardstream
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
package com.cardstream;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.nio.ByteBuffer;

public class Transaction {

	public enum ACTION {
		PREAUTH,
		SALE,
		REFUND,
		REFUND_SALE,
		VERIFY,
		CAPTURE,
		CANCEL
	}

	public enum TYPE {
		ECOM(1),
		MOTO(2),
		CA(9);

		private final int id;
		TYPE(int id) { this.id = id; }
		public int getValue() { return id; }
	}

	private String reqString = "";
	private String resString = "";
	private Boolean httpSuccess = false;
	private final HashMap<String, String> resCol = new HashMap<>();
	private final TreeMap<String, String> formData = new TreeMap<>();

	// Merchant details
	private final String GATEWAY_URL;
	private String merchantID = "";
	private String merchantPassword = "";
	private String preSharedKey;

	// Order fields
	private String action;
	private String type;
	private String currencyCode;
	private String countryCode;
	private String amount;
	private String xref;
	private String orderRef;
	private String callbackURL;
	private String transactionUnique;


	// Card details
	private String cardCVV;
	private String cardNumber;
	private String cardExpiryDate;
	private String cardExpiryMonth;
	private String cardExpiryYear;
	private String cardStartYear;
	private String cardStartMonth;
	private String cardIssueNumber;

	// Customer details
	private String customerName;
	private String customerCompany;
	private String customerAddress;
	private String customerTown;
	private String customerCounty;
	private String customerPostcode;
	private String customerCountryCode;
	private String customerPhone;
	private String customerEmail;

	private String taxDiscountDescription;


	public Transaction(String url, String merchantID, String preSharedKey) {
		GATEWAY_URL = url;
		try {
			setMerchantID(merchantID);
		} catch (Exception e) {
			// Leave until authorisation to throw exceptions
		}
		this.preSharedKey = preSharedKey;
	}

	public Transaction(String url, String merchantId, String preSharedKey, String merchantPassword) {
		GATEWAY_URL = url;
		try {
			setMerchantID(merchantID);
		} catch (Exception e) {
			// Leave until authorisation to throw exceptions
		}
		this.preSharedKey = preSharedKey;
		this.merchantPassword = merchantPassword;
	}

	public Boolean authorise() throws Exception {
		try {
			// Build the form, ensuring the necessary parameters have been defined
			buildForm();

			// Send the form
			if (!sendForm()) {
				return false;
			}

			// Parse the response
			ParseResponse();

			return true;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	private void buildForm() throws Exception {

		// Make sure transaction is always unique
		if (transactionUnique == null || transactionUnique.length() == 0) {
			setTransactionUnique(generateUniqid());
		}

		// Make sure the current values are valid
		try {
			setAmount(amount);
			setCardNumber(cardNumber);
			setCustomerName(customerName);
			setCardExpiryYear(cardExpiryYear);
			setCardExpiryMonth(cardExpiryMonth);
			setCurrencyCode(currencyCode);
			setCountryCode(countryCode);
		} catch (Exception e) {
			throw e;
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

	private boolean sendForm() {
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

	public String generateUniqid(String prefix, boolean moreEntropy) {
		long time = System.currentTimeMillis();
		String uniqid = "";
		if (!moreEntropy) {
			uniqid = String.format("%s%08x%05x", prefix, time / 1000, time);
		} else {
			SecureRandom sec = new SecureRandom();
			byte[] sbuf = sec.generateSeed(8);
			ByteBuffer bb = ByteBuffer.wrap(sbuf);

			uniqid = String.format("%s%08x%05x", prefix, time / 1000, time);
			uniqid += "." + String.format("%.8s", "" + bb.getLong() * -1);
		}

		return uniqid;
	}

	public String generateUniqid() {
		return generateUniqid("", false);
	}

	public void setMerchantID(String merchantID) throws Exception {
		if (merchantID == null || merchantID.length() == 0) {
			throw new Exception("Merchant ID must not be null!");
		} else {
			this.formData.put("merchantID", merchantID);
			this.merchantID = merchantID;
		}
	}

	public void setPreSharedKey(String preSharedKey) {
		this.preSharedKey = preSharedKey;
	}

	public void setAmount(int amount) throws Exception {
		if (amount < 10) {
			throw new Exception("amount must be over 10");
		} else {
			this.formData.put("amount", Integer.toString(amount));
			this.amount = Integer.toString(amount);
		}
	}

	public void setAmount(String amount) throws Exception {
		try {
			if (amount.matches("[0-9]+\\.[0-9]+")) {
				this.setAmount(Float.parseFloat(amount));
			} else if (amount.matches("[0-9]+")) {
				this.setAmount(Integer.parseInt(amount));
			} else {
				throw new Exception("amount must be in a valid format");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public void setAmount(float amount) throws Exception {
		try {
			this.setAmount((int)(amount * 100));
		} catch (Exception e) {
			throw e;
		}
	}

	public void setCountryCode(String countryCode) throws Exception {
		if (countryCode.toUpperCase().matches("([A-Z]{2,3}|[0-9]{3})")) {
			this.formData.put("countryCode", "" + countryCode.toUpperCase());
			this.countryCode = countryCode.toUpperCase();
		} else {
			throw new Exception("countryCode must match an ISO Alpha-2, 3 or Numeric code (e.g. GB/GBR/826)");
		}
	}

	public void setCountryCode(int countryCode) throws Exception {
		try {
			this.setCountryCode(Integer.toString(countryCode));
		} catch (Exception e) {
			throw e;
		}
	}

	public void setCurrencyCode(String currencyCode) throws Exception {
		if (currencyCode.toUpperCase().matches("([A-Z]{3}|[0-9]{3})")) {
			this.formData.put("currencyCode", "" + currencyCode.toUpperCase());
			this.currencyCode = currencyCode.toUpperCase();
		} else {
			throw new Exception("currencyCode must match an ISO 4217 Numeric or Alphabetic code (e.g. 826/GBP)");
		}
	}

	public void setCurrencyCode(int currencyCode) throws Exception {
		try {
			this.setCurrencyCode(Integer.toString(currencyCode));
		} catch (Exception e) {
			throw e;
		}
	}

	public void setTransactionUnique(String transactionUnique) {
		this.formData.put("transactionUnique", transactionUnique);
		this.transactionUnique = transactionUnique;
	}


	public void setCallbackURL(String callbackURL) {
		this.formData.put("callbackURL", callbackURL);
		this.callbackURL = callbackURL;
	}

	public void setType(TYPE type) {
		this.formData.put("type", Integer.toString(type.getValue()));
		this.type = Integer.toString(type.getValue());
	}

	public void setAction(ACTION action) {
		this.formData.put("action", action.name());
		this.action = action.name();
	}

	public void setOrderRef(String ref) {
		this.formData.put("orderRef", ref);
		this.orderRef = ref;
	}

	public void setXref(String xref) {
		this.formData.put("xref", xref);
		this.xref = xref;
	}

	public void setCardCVV(String cardCVV) throws Exception {
		if (cardCVV.matches("([0-9]{2,3})")) {
			this.formData.put("cardCVV", cardCVV);
			this.cardCVV = cardCVV;
		} else {
			throw new Exception("cardCVV must be a 2-3 digit number");
		}
	}

	public void setCardNumber(String cardNumber) throws Exception {
		if (cardNumber.matches("([0-9]{4}[ ]?){3}([0-9]{3,7})")) {
			this.formData.put("cardNumber", cardNumber);
			this.cardNumber = cardNumber;
		} else {
			throw new Exception("cardNumber only allows a valid numeric set");
		}
	}

	public void setCardExpiryDate(String cardExpiryDate) throws Exception {
		if (cardExpiryDate.matches("([0-9]{2}[-/]?[0-9]{2}|[0-9]{2}[/]?[0-9]{4})")) {
			this.formData.put("cardExpiryDate", cardExpiryDate);
			this.cardExpiryDate = cardExpiryDate;
		} else {
			throw new Exception("cardExpiryDate must be in format MMYY, MM/YY, MM-YY, MMYYYY, or MM/YYYY");
		}
	}

	private Boolean isValidFutureDate(int MM, int YY) {
		int currentYear = Integer.parseInt(Integer.toString((Calendar.getInstance()).get(Calendar.YEAR)).substring(2));
		int currentMonth = (Calendar.getInstance()).get(Calendar.MONTH) + 1; // Calendar uses 0 index instead of 1

		return (
			(MM > 0 && MM < 13) &&
			(YY > -1 && YY < 100) &&
			!(YY <= currentYear && MM <= currentMonth)
		);
	}

	public void setCardExpiryMonth(String cardExpiryMonth) throws Exception {
		try {
			int month = Integer.parseInt(cardExpiryMonth);

			if (
				!(this.cardExpiryYear == null || this.cardExpiryYear == "") &&
				!this.isValidFutureDate(month, Integer.parseInt(this.cardExpiryYear))) {
				String error = "Setting cardExpiryMonth:- Expiry cannot be set to a date in the past (expiry month %s, expiry year %s)";
				throw new Exception(String.format(error, month, this.cardExpiryYear));
			} else if (month > 12 || month < 1) {
				throw new Exception("cardExpiryMonth must be a valid numeric month");
			} else {
				String strMonth = String.format("%02d", month);
				this.formData.put("cardExpiryMonth", strMonth);
				this.cardExpiryMonth = strMonth;
			}
		} catch (Exception e) {
			// Number format exception or null pointer
			 throw e;
		}
	}

	public void setCardExpiryMonth(int cardExpiryMonth) throws Exception {
		try {
			this.setCardExpiryMonth(Integer.toString(cardExpiryMonth));
		} catch (Exception e) {
			throw e;
		}
	}

	public void setCardExpiryYear(String cardExpiryYear) throws Exception{
			try {
			int year = Integer.parseInt(cardExpiryYear);

			if (
				!(this.cardExpiryMonth == null || this.cardExpiryMonth == "") &&
				!this.isValidFutureDate(Integer.parseInt(this.cardExpiryMonth), year)
			) {
				String error = "Setting cardExpiryYear:- Expiry cannot be set to a date in the past (expiry month %s, expiry year %s)";
				throw new Exception(String.format(error, this.cardExpiryMonth, year));
			} else if (year < 0 || year > 100) {
				throw new Exception("cardExpiryYear must be a valid 2-digit year");
			} else {
				String strYear = String.format("%02d", year);
				this.formData.put("cardExpiryYear", cardExpiryYear);
				this.cardExpiryYear = cardExpiryYear;
			}
		} catch (Exception e) {
			// Number format exception or null pointer
			 throw e;
		}
	}

	public void setCardExpiryYear(int cardExpiryYear) throws Exception {
		try {
			this.setCardExpiryYear(Integer.toString(cardExpiryYear));
		} catch (Exception e) {
			throw e;
		}
	}

	public void setCardStartYear(String cardStartYear) {
		this.formData.put("cardStartYear", cardStartYear);
		this.cardStartYear = cardStartYear;
	}

	public void setCardStartYear(int cardStartYear) {
		this.setCardStartYear(Integer.toString(cardStartYear));
	}

	public void setCardStartMonth(String cardStartMonth) {
		this.formData.put("cardStartMonth", cardStartMonth);
		this.cardStartMonth = cardStartMonth;
	}

	public void setCardStartMonth(int cardStartMonth) {
		this.setCardStartMonth(Integer.toString(cardStartMonth));
	}

	public void setCardIssueNumber(String cardIssueNumber) {
		this.formData.put("cardIssueNumber", cardIssueNumber);
		this.cardIssueNumber = cardIssueNumber;
	}

	public void setCustomerName(String customerName) throws Exception {
		if (customerName == null || customerName.length() == 0) {
			throw new Exception("customerName must not be blank!");
		} else {
			this.formData.put("customerName", customerName);
			this.customerName = customerName;
		}
	}

	public void setCustomerCompany(String customerCompany) {
		this.formData.put("customerCompany", customerCompany);
		this.customerCompany = customerCompany;
	}

	public void setCustomerAddress(String customerAddress) {
		this.formData.put("customerAddress", customerAddress);
		this.customerAddress = customerAddress;
	}

	public void setCustomerTown(String customerTown) {
		this.formData.put("customerTown", customerTown);
		this.customerTown = customerTown;
	}

	public void setCustomerCounty(String customerCounty) {
		this.formData.put("customerCounty", customerCounty);
		this.customerCounty = customerCounty;
	}

	public void setCustomerCountryCode(String customerCountryCode) throws Exception {
		if (customerCountryCode.toUpperCase().matches("([A-Z]{2,3}|[0-9]{3})")) {
			this.formData.put("customerCountryCode", customerCountryCode);
			this.customerCountryCode = customerCountryCode;
		} else {
			throw new Exception("customerCountryCode must match an ISO Alpha-2, 3 or Numeric code (e.g. GB/GBR/826)");
		}
	}

	public void setCustomerCountryCode(int customerCountryCode) throws Exception {
		try {
			this.setCustomerCountryCode(Integer.toString(customerCountryCode));
		} catch (Exception e) {
			throw e;
		}
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
		//  throw new UnsupportedOperationException("Not supported yet.");
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

	public String getAuthxref() {
		return this.resCol.get("xref");
	}

	public String getAuthOrderDescription() {
		return this.resCol.get("orderDesc");
	}

	public String getAuthTransactionUnique() {
		return this.resCol.get("transactionUnique");
	}

}
