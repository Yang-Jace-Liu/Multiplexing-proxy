from distutils.core import setup
from setuptools import find_packages

setup(name='multiplexing',
      version='1.0',
      description='Multiplexing proxy server and client',
      author='Yang Liu',
      author_email='yang.jace.liu@linux.com',
      scripts=["scripts/proxyclient", "scripts/proxyserver"],
      packages=find_packages(),
      )
