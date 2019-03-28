class Data(object):
    _instance = None

    def __new__(cls, *args, **kwargs):
        if Data._instance is None:
            Data._instance = object.__new__(cls, *args, **kwargs)
        return Data._instance

    def __init__(self):
        self.link_threads = []
        self.service_thread = None
        self.writer_thread = None
