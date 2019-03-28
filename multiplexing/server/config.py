class Config:
    _DEFAULT_SERVICE_HOSTNAME = "127.0.0.1"
    _DEFAULT_SERVICE_PORT = 80
    _DEFAULT_PROXY_PORT = 2000

    def __init__(self, *args, **kwargs):
        self.service_hostname = kwargs.get("service_hostname", Config._DEFAULT_SERVICE_HOSTNAME)
        self.service_port = kwargs.get("service_port", Config._DEFAULT_SERVICE_PORT)
        self.proxy_port = kwargs.get("proxy_port", Config._DEFAULT_PROXY_PORT)
