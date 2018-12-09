
import {BehaviorSubject, Subject} from 'rxjs';
import {filter, mapTo, take} from 'rxjs/operators';

class Socket {
  
  isReady = new BehaviorSubject(false);
  messages = new Subject();
  
  connectSocket = () => {
    console.log('Connecting to socket');
    this.socket = new WebSocket(`ws://${window.location.hostname}:${window.location.port}/socket`)
    this.socket.onopen = () => this.isReady.next(true);
    this.socket.onclose = () => {
      console.log("Socket closed");
      this.isReady.next(false);
      if(!window.closing)
        setTimeout(this.connectSocket, 500)
      
    };
    this.socket.onerror = () => {
      console.log("Socket error!!!");
      this.isReady.next(false);
    };
    this.socket.onmessage = message => this.messages.next(message.data);
  };
  
  constructor() {
    this.connectSocket();
  }
  
  whenReady = () => this.isReady.pipe(
    filter(Boolean),
    mapTo(this),
    take(1)
  );
  
  send = message => {
    const s = this.whenReady().subscribe(t => {
      t.socket.send(message);
      s.unsubscribe();
    });
  }
}
const socket = new Socket();
export default socket;
