import { BehaviorSubject, Subject } from 'rxjs';
import { filter, mapTo, take, takeWhile } from 'rxjs/operators';
import {interval} from 'rxjs';

class Socket {
  
  isReady = new BehaviorSubject(false);
  messages = new Subject();
  
  connectSocket = () => {
    console.log('Connecting to socket');
    this.socket = new WebSocket(`ws://${window.location.hostname}:${serverPort}/socket`);
    this.socket.onopen = () => {
      this.isReady.next(true);
      this.socket.send('beat');
      interval(10000).pipe(
        mapTo(this),
        takeWhile(t => t.isReady.value)
      ).subscribe(t => t.socket.send('beat'));
    };
    this.socket.onclose = () => {
      console.log("Socket closed");
      this.isReady.next(false);
      if (!window.closing)
        setTimeout(this.connectSocket, 100)
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
  
  send = message => this.whenReady().subscribe(t => t.socket.send(message));
}

const socket = new Socket();
export default socket;
