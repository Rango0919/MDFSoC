<?php

class ApiException extends Exception
{
    public string $errorCode;
    public string $errorSource;
    public int $httpStatus;

    public function __construct(string $message, string $errorCode = 'upstream_error', string $errorSource = 'backend', int $httpStatus = 502)
    {
        parent::__construct($message);
        $this->errorCode = $errorCode;
        $this->errorSource = $errorSource;
        $this->httpStatus = $httpStatus;
    }
}

class UpstreamException extends ApiException
{
    public function __construct(string $message, string $errorSource = 'backend')
    {
        parent::__construct($message, 'upstream_error', $errorSource, 502);
    }
}

class RateLimitException extends ApiException
{
    public function __construct(string $message = 'Rate limit reached', string $errorSource = 'backend')
    {
        parent::__construct($message, 'rate_limited', $errorSource, 429);
    }
}