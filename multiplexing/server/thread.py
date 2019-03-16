import socket
from threading import Thread
from multiprocessing


class LinkThread(Thread):
    def __init__(self, sock: socket.socket):
        super().__init__()
        self.socket = sock

        # Set the socket to non-blocking
        self.socket.setblocking(False)

    def run(self):
        pass # TODO: implement this


    def add_task(self):
        pass
