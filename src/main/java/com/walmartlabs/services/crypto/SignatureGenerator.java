package com.walmartlabs.services.crypto;

import java.io.ObjectStreamException;
import java.security.KeyRep;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

/**
 * Signature generator
 *
 */
@Component
public class SignatureGenerator {
	public Map<String, String> generateSignature(String consumerId) throws Exception {
		final Map<String, String> map = new HashMap<>();
		final String priviateKeyVersion = "1";
		final String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCirevCmgWmvqvYyTQoo8yMf7ft4nt2/hqVDkrdThM4HZdSeABUUiMI4kyScvKRPRUSLEAhmLspsIz/tvq9aAZCNQ+ciXcFmno8h3rv4XaWRvE8dVHpTL7itI39fkGg+x0KdmNwxuYK63pVzDZHl5thCeI05HyO/N7o1iTrL1LRq1mlhiqJ4ohhYtD4y2YUfyx9NZInyAzN4JdS25EJjx9muPONUTyJ0+ItaX3Mtmlj6X5jOvrSuqTimoSqArBHO3gRxa2dn0CTY9OjPeuFsWqS2pEaURCWAZ4Otfj6cVCdlkIlVG/pRravt8sYNmLDaqLNSo+bUY1aoptiAP3PNCSdAgMBAAECggEABWV7kqMFWPiuPqy9/DMiz+5UraF7swDO5O7qcNjsLkpdALFWYXWwD9Vh8OG7RjIehtHM9tBYNRPUOY9aVfojawLTl7+/oQH02BkubNKv2mBUFEtYAkM3edG/fA86IhryZaxztAQFEYuqLTpu+oT+9IMda9/AegXNZy/jXaHeP3JAIrBUJcu5DxSFORJR6vz2HSW3tTtvcdZGFjJkyrCkzcEU9W0FU7sgZqsXXiwXLJHzJJ+Zdi9IZWvsMmkSCeXsXMBtzUSdXfhkqJFh03N39nBscEdZqxMcX5ttHOic5en/j3WMOBj0JLvmeH/Heq0VaWwOYQBABDh1Tz5LmEY8CQKBgQDWfi70GtB4NeDYnTqcyqQlCR6F2FtyVOSFHSrYWebdyVVN2TcxoFPFy/DFdMiaFO2SW9c9SILTC5EmFeSobPxWasE1pqb7ahWcmrBrICsvKZvf3DNVUCuWH/np+QSWetrwZw/CrRCcvrR7S+HPwr0eqLucJmyNl8k3H3Vk12zbzwKBgQDCKO9BBGZGenQKEatIT2d/yV1dcxZTq2rFJHEEZJiJZg7HjqhwF9e0i3PFJdQ3U4/bPXg4RC8JkrNz+If1IhTxw6gVUkyCFvbEOgFTjSxWCOAmNW1XIXnGnJCjzw7H9/+t5Epd6PAgVkb/NN4dbeNwEJpHaM82WBtj0vW2z4a30wKBgBgeHhdygGhT3pFctH92xZgoe1cfacDkTMGu8udazHu5rK7RMsSQu3qtIMiDBh9VJRVk4EHSymsCjPKUWZ6aipEI9eqbb4Erf3yZZCbXeiOWcSFtuBPqrMv3knk9d8eXztBjNkF0hj6prs+CS8S5p9wvqtC2/VO6cnSRQ/jvxBy/AoGBAIodzHSXyJmCKmGvSATcV2fTupLrd4p1ejJbRfo2BTxYWVMj7DYw/8TzHpuz2U6yJrdy9r4v4rYQoY9x0GVUpUmGQV9JeBticpAMIz1oQ43AKpODlhBRQ+tBHLMqa/1cMYllyHEdbxPRzBdnaEnPDed+KAF8UUr7SiCsxRYScmb5AoGAXzoXEBgo/RI44C4gwf1yErM/HuwOJwb890uqHhg2CJ2bTQ9X1mV5GnVoGjhKxjxm67RWeQrKkW0FDYmV+T3MT3C0jpWJ88zc2yAsDOEnQRCdcoEakIJghKXXNlEK/tdQnt8j1Zk7Pi1X4Q7dwYkdm/XBZIYaMJsWf8HeAeuqmks=";
		final long intimestamp = DateUtils.addMinutes(new Date(), 5).getTime();

		map.put("WM_CONSUMER.ID", consumerId);
		map.put("WM_CONSUMER.INTIMESTAMP", Long.toString(intimestamp));
		map.put("WM_SEC.KEY_VERSION", priviateKeyVersion);

		final String[] array = canonicalize(map);

		final String data = generateSignature(privateKey, array[1]);

		map.put("WM_SEC.AUTH_SIGNATURE", data);
		return map;
	}

	public String generateSignature(final String key, final String stringToSign) throws Exception {
		final Signature signatureInstance = Signature.getInstance("SHA256WithRSA");

		final ServiceKeyRep keyRep = new ServiceKeyRep(KeyRep.Type.PRIVATE, "RSA", "PKCS#8", Base64.decodeBase64(key));

		final PrivateKey resolvedPrivateKey = (PrivateKey) keyRep.readResolve();

		signatureInstance.initSign(resolvedPrivateKey);

		final byte[] bytesToSign = stringToSign.getBytes("UTF-8");
		signatureInstance.update(bytesToSign);
		final byte[] signatureBytes = signatureInstance.sign();

		final String signatureString = Base64.encodeBase64String(signatureBytes);

		return signatureString;
	}

	protected static String[] canonicalize(final Map<String, String> headersToSign) {
		final StringBuffer canonicalizedStrBuffer = new StringBuffer();
		final StringBuffer parameterNamesBuffer = new StringBuffer();
		final Set<String> keySet = headersToSign.keySet();

		// Create sorted key set to enforce order on the key names
		final SortedSet<String> sortedKeySet = new TreeSet<>(keySet);
		for (final String key : sortedKeySet) {
			final Object val = headersToSign.get(key);
			parameterNamesBuffer.append(key.trim()).append(";");
			canonicalizedStrBuffer.append(val.toString().trim()).append("\n");
		}

		return new String[] { parameterNamesBuffer.toString(), canonicalizedStrBuffer.toString() };
	}

	class ServiceKeyRep extends KeyRep {
		private static final long serialVersionUID = -7213340660431987616L;

		public ServiceKeyRep(final Type type, final String algorithm, final String format, final byte[] encoded) {
			super(type, algorithm, format, encoded);
		}

		@Override
		protected Object readResolve() throws ObjectStreamException {
			return super.readResolve();
		}
	}
}
