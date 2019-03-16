class Config:
    DEFAULT_PROXY_PORT = 3000
    DEFAULT_SERVER_HOST = "localhost"
    DEFAULT_SERVER_PORT = 2000
    DEFAULT_NUM_CONNECTIONS = 2

    def __init__(self, *args, **kwargs):
        self.server_host = kwargs.get("server_host", Config.DEFAULT_SERVER_HOST)
        self.server_port = kwargs.get("server_port", Config.DEFAULT_SERVER_PORT)
        self.proxy_port = kwargs.get("proxy_port", Config.DEFAULT_PROXY_PORT)
        self.num_connections = kwargs.get("num_connections", Config.DEFAULT_NUM_CONNECTIONS)
