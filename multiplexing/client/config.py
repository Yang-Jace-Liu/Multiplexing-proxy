class Config:
    _DEFAULT_PROXY_SERVER_HOSTNAME = "127.0.0.1"
    _DEFAULT_PROXY_SERVER_PORT = 2000
    _DEFAULT_LISTENING_PORT = 3000
    _DEFAULT_LINK_NUM = 2

    def __init__(self, *args, **kwargs):
        self.proxy_server_hostname = kwargs.get("proxy_server_hostname", Config._DEFAULT_PROXY_SERVER_HOSTNAME)
        self.proxy_server_port = kwargs.get("proxy_server_port", Config._DEFAULT_PROXY_SERVER_PORT)
        self.listening_port = kwargs.get("listening_port", Config._DEFAULT_LISTENING_PORT)
        self.link_num = kwargs.get("link_num", Config._DEFAULT_LINK_NUM)
