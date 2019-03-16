from multiprocessing import Lock


class Data:
    id = 0
    mutex = Lock()

    @staticmethod
    def get_new_id() -> int:
        with Data.mutex:
            Data.id += 1
            return Data.id


