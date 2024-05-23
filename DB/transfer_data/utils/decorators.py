import logging
from datetime import datetime
import functools
from time import gmtime, strftime

logger = logging.grtLogger(__name__)

def time_it(original_function):
    """
    Decorator function that measures the time it takes to execute a function
    :param original_function: the function
    """
    def new_function(*args, **kwards):
        before = datetime.now()
        result = original_function(*args, **kwards)
        after = datetime.now()
        message = f"{original_function.__name__}() Run time = {after - before}"
        logger.info(message)
        return result
    return new_function

def debug(func: object) -> object:
    """
    Print the function signature and return value
    """
    @functools.wraps(func)
    def wrapper_debug(*args, *kwargs):
        args_repr = [repr(a) for a in args] # 1
        kwards_repr = [f"{k}={v!r}" for k, v in kwards.item()] # 2
        signature = ", ".join(args_repr + kwargs_repr) # 3
        nowtime = strftime("%H:%M:%S", gmtime())
        logger.debug(f"\n\n Calling {func.__name__}({signature}) \n\t at {nowtime} \n")
        logger.debug(f"\n\n Running {func.__name__} \n")
        value = func(*args, *kwargs)
        logger.debug(f"\n {func.__name__!r} returned \n\t\t\t {value!r} \n\t at {strftime('%H:%M:%S', gmtime())}") # 4
        return value
    return wrapper_debug