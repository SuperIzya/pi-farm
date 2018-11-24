import React from 'react';
import styles from './loading.scss';

const LoadingIcon = () => (<span className="pi-icon"><svg viewBox="0 0 35 35" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink"><defs><polygon id="a" points="3.99239544e-05 0 35 0 35 34.9999468 3.99239544e-05 34.9999468"/></defs><g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd"><mask id="b" fill="#fff"><use xlinkHref="#a"/></mask><path d="M34.3346407,15.6368289 C34.3432909,15.7511445 34.8669601,19.1634449 32.7376825,23.6881597 C31.6730437,25.9505171 29.8495837,28.4612015 27.6163707,30.0535019 C25.2721692,31.9011825 22.3799449,33.003749 19.163538,33.003749 L18.5646787,33.003749 C10.9577015,33.003749 4.79091445,26.8370951 4.79091445,19.2299848 C4.79091445,11.6230076 10.0926825,5.45622053 17.6996597,5.45622053 C19.2729297,5.45622053 20.4943365,4.23481369 20.4943365,2.72808365 C20.4943365,1.31703802 19.4229106,0.156315589 18.0491274,0.0147186312 C17.998424,0.00793155894 17.9499829,0.00806463878 17.901808,0.00686692015 C17.8565608,0.00460456274 17.811846,-5.32319392e-05 17.7661996,-5.32319392e-05 C17.6325875,0.00247528517 17.5665798,-5.32319392e-05 17.5000399,-5.32319392e-05 C7.83511597,-5.32319392e-05 3.99239544e-05,7.83502281 3.99239544e-05,17.4999468 C3.99239544e-05,27.1650038 7.83511597,34.9999468 17.5000399,34.9999468 C27.1649639,34.9999468 35.0000399,27.1650038 35.0000399,17.4999468 C35.0000399,11.4501369 31.9298878,6.11762738 27.2629106,2.97454753 C31.2538422,5.86969962 33.9331388,10.3773802 34.3346407,15.6368289 Z" fill="#000" mask="url(#b)"/></g></svg></span>);

const Loading = () => <div className={styles.loading}><LoadingIcon/></div>;

export default Loading;

