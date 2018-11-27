
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
    this.socket.onmessage = message => this.messages.next(message.data);
  }
  
  whenReady = () => this.isReady.pipe(
    filter(Boolean),
    mapTo(this)
  );
  
  send = message => this.whenReady().subscribe(t => t.socket.send(message));
}
const socket = new Socket();
export default socket;
