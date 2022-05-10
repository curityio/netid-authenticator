package io.curity.authenticator.netid;

public class SSLContextException extends RuntimeException
{
    public SSLContextException()
    {
        super("Failed to create the SSL Context");
    }
}
