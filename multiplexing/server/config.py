class Config:
    DEFAULT_SERVICE_PORT = 8000
    DEFAULT_SERVICE_HOST = "127.0.0.1"
    DEFAULT_PROXY_PORT = 2000
    DEFAULT_NUM_CONNECTIONS = 2

    def __init__(self, *args, **kwargs):
        self.service_port = kwargs.get("service_port", Config.DEFAULT_SERVICE_PORT)
        self.service_host = kwargs.get("service_host", Config.DEFAULT_SERVICE_HOST)
        self.proxy_port = kwargs.get("proxy_port", Config.DEFAULT_PROXY_PORT)
        self.num_connections = kwargs.get("num_connections", Config.DEFAULT_NUM_CONNECTIONS)
