import {from} from 'rxjs';
import axios from 'axios';

class ClientClass {
  client = axios.create({
    baseURL: '/',
    'Access-Control-Allow-Origin': '*'
  });
  
  get = (url, options = {}) => from(this.client.get(url, options));
  delete = (url, options = {}) => from(this.client.delete(url, options));
  request = options => from(this.client.request(options));
  head = (url, options = {}) => from(this.client.head(url, options));
  options = (url, options = {}) => from(this.client.options(url, options));
  post = (url, data, options = {}) => from(this.client.post(url, data, options));
  put = (url, data, options = {}) => from(this.client.put(url, data, options));
  patch = (url, data, options = {}) => from(this.client.patch(url, data, options));
}

const Client = new ClientClass();
export default Client;
