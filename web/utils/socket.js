
import {BehaviorSubject, Subject} from 'rxjs';
import {filter, mapTo} from 'rxjs/operators';

class Socket {
  
  isReady = new BehaviorSubject(false);
  messages = new Subject();
  
  constructor() {
    this.socket = new WebSocket(`ws://${window.location.hostname}:${window.location.port}/socket`)
    this.socket.onopen = () => this.isReady.next(true);
    this.socket.onclose = () => this.isReady.next(false);
    this.socket.onerror = () => this.isReady.next(false);
    this.socket.onmessage = (socket, evt) => this.messages.next(evt);
  }
  
  whenReady = () => this.isReady.pipe(
    filter(Boolean),
    mapTo(this)
  );
  
  send = message => this.whenReady().subscribe(() => this.socket.send(message));
}
const socket = new Socket();
export default socket;
