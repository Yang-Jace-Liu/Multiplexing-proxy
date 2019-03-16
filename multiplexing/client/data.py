from multiprocessing import Lock
import random


class Data:
    id = 0
    mutex = Lock()

    client_thread = None
    link_thread = []

    @staticmethod
    def get_new_id() -> int:
        with Data.mutex:
            Data.id += 1
            return Data.id

    @staticmethod
    def send_to_link_thread(task):
        l = len(Data.link_thread)
        ind = random.randint(0, l-1)
        Data.link_thread[ind].add_task(task)
