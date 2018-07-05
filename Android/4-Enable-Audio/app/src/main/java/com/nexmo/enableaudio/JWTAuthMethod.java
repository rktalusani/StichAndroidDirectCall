/*
 * Copyright (c) 2011-2017 Nexmo Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nexmo.enableaudio;

import android.util.Base64;
import android.util.Log;

import com.auth0.jwt.Algorithm;
import com.auth0.jwt.JWTSigner;
import com.nexmo.client.NexmoUnexpectedException;
import com.nexmo.client.auth.AbstractAuthMethod;

import org.apache.http.client.methods.RequestBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JWTAuthMethod extends AbstractAuthMethod {
    private static final Pattern pemPattern = Pattern.compile(
            "-----BEGIN PRIVATE KEY-----" + // File header
                    "(.*\\n)" +                     // Key data
                    "-----END PRIVATE KEY-----" +   // File footer
                    "\\n?",                         // Optional trailing line break
            Pattern.MULTILINE | Pattern.DOTALL);
    public final int SORT_KEY = 10;
    private String applicationId;
    private String name;
    private JWTSigner signer;

    public JWTAuthMethod(final String applicationId,  final String name)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        this.applicationId = applicationId;
        this.name = name;
        byte[] decodedPrivateKey = decodePrivateKey();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedPrivateKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey key = kf.generatePrivate(spec);
        this.signer = new JWTSigner(key);
    }



    public static String constructJTI() {
        return UUID.randomUUID().toString();
    }

    protected byte[] decodePrivateKey() throws InvalidKeyException {
        try {

            String text= "-----BEGIN PRIVATE KEY-----\n" +
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC3CYO1I83KHvvf\n" +
                    "Q4mYsIrz1ZEaCWEUiFg38Q2D+J0cAaGTm5G1/LgJ1qzL4M8veEmihgkYYFjIBHk8\n" +
                    "jsawKCqn/zMz6+NIJFWFkMrztVA5extI8Z/qJRg/Jf4i/+fxW5q3awltelZjMtVF\n" +
                    "v5aealK1qchlhBcQ38hpJ1LFL13lLdKZu2kxCLnGJng269bGM08ygghz7UVyT+w0\n" +
                    "mjTehQg7nbkFvKvzz1/0stlNFLGaYFaScbMqbgpVkByACioFcLXKcIIGr7GVm6NY\n" +
                    "N1zz3ViuuScIdLR6VgpuOYq9+rVLhYocx4WlQnu6Da57M/CBiFOsqK2Ld2Z8cy2I\n" +
                    "Q1npO4UnAgMBAAECggEAJFJKBY8GTj6ZwrM3Qcp+uGY/9ge4cQehYfB+uxqBqsYU\n" +
                    "FyN+5bsxlho4jfidhJD9I7gvY9vyojZpDIBUoy5FULMOqRX9MxqqseeKrpPjSJTK\n" +
                    "VE5GaoNT7WwPo0he8YE5EX63DzeAnwy+T6n2LJdytEPt1V9B6IJP1bYM8b41hr2b\n" +
                    "c+Dneeo52hb2IYAJ6gr6NVrwNV7v/Tjyg1CKtE3+1yXGAjRVkBwuDooWOoW5DewI\n" +
                    "ZvZEB3K5xYPBO8AJYNXE1O7sZaHRVNj3Uq0bm6AiwZ/rx5UVLFANJgIVS/CaAt4e\n" +
                    "S1QG6hvFLP7LNJtdjbn/vpTLGYgQFjKnjnZKiQ1Y8QKBgQD2agpvA7ahu8OwjCQo\n" +
                    "6VpgdBkjmVszB2YFMF6o5gltb8vbpLsKPk16ePDLihxTMDBly1FUIjaoaMZGErJJ\n" +
                    "ySmD/VaSj+grY0kRMIJ6gYZVoGI9A/5hEKal/taFSN7KgNnjjwEBwxXskhYmv1Di\n" +
                    "jL4Y15oESAniYXDY+EtP1hOW6QKBgQC+KFJ7yO8zzzylcinRr/LaOcpwv0PWdGwz\n" +
                    "bXJKJdyv2yRhTnTj8Hzps5TDhLpexq8jDYGD/GkK8/uQ8yKWOS9IvWDGNql3YWwZ\n" +
                    "QiyP6e4Bi4VmygBSFg9YqZcKxZoY515JUW926Jj1G6UWKjBffROBvrRamMcRxXvo\n" +
                    "eKA0w6bRjwKBgQDIhE3tPKZXQgXiGogqSonix1bVoyuVgMXCHzRIWLj/NuQ63ffe\n" +
                    "RAikStoXp1GZWDNqAmMyBY1Yybnet8QhSvLflz3lkfkppWeF92WV7uSztQl8AU31\n" +
                    "CqmxlohWeY/iVav5AxucpXWnxtEDwtc9I65lZLzOExkIAKblyFbwppHWUQKBgB1l\n" +
                    "9nqUsgZ8f4/EepqkbRM35AhSSZaaITBCN20nNVsqshEpJAEpaOZokRM5LecBKIpu\n" +
                    "yBszprm+xqG+NRaseJbyUEfUo5aFRUUSaflF3Tn37APcLviB2JWIgoVzz0MSYg1L\n" +
                    "vcPQafVa9MbcduYMXDuu1As9m2kv8twZ6gq+RMgZAoGAEJjP3tjY+IH30R23yazZ\n" +
                    "lvTWf9vsi8vOjDgCNimMQEYHarBLekY4f8WpvFDoRhtOdMxcNosfPVtwcIfp3ezm\n" +
                    "U1lNRUEz9ds28wUoNlPdc1221Je3t0Iz3e1wMjtjmair1BxUdECFKd7aFbn+NLyy\n" +
                    "wCB5iIQq38DVlhsBxhky4s0=\n" +
                    "-----END PRIVATE KEY-----";
            String s = new String(text.getBytes(), "UTF-8");
            Matcher extracter = pemPattern.matcher(s);
            if (extracter.matches()) {
                String pemBody = extracter.group(1);
                Log.d("PEMBODY",pemBody);
                return Base64.decode(pemBody,Base64.DEFAULT);
            } else {
                throw new InvalidKeyException("Private key should be provided in PEM format!");
            }
        } catch (UnsupportedEncodingException exc) {
            // This should never happen.
            throw new NexmoUnexpectedException("UTF-8 is an unsupported encoding in this JVM", exc);
        }
    }

    @Override
    public RequestBuilder apply(RequestBuilder request) {
        String token = this.constructToken(
                System.currentTimeMillis() / 1000L,
                constructJTI());
        request.setHeader("Authorization", "Bearer " + token);
        return request;
    }
    public String getToken() {
        String token = this.constructToken(
                System.currentTimeMillis() / 1000L,
                constructJTI());
        return token;
    }
    public String constructToken(long iat, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iat", iat);
        claims.put("application_id", this.applicationId);
        claims.put("jti", jti);
        claims.put("sub",name);
        claims.put("acl","{ZpathsZ: {Z/v1/sessions/**Z: {}, Z/v1/users/**Z: {}, Z/v1/conversations/**Z: {}}}".replace('Z','\''));

        JWTSigner.Options options = new JWTSigner.Options()
                .setAlgorithm(Algorithm.RS256);
        String signed = this.signer.sign(claims, options);

        return signed;
    }

    @Override
    public int getSortKey() {
        return this.SORT_KEY;
    }
}