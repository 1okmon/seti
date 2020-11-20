import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
public class Station implements Runnable{

    private DatagramSocket socket;
    private HashMap<InetSocketAddress, NodeInfo> communicator;
    private final static int BUF_SIZE = 1024 * 10;
    private InetSocketAddress parent;
    private InetSocketAddress reserveToSend;
    private String msg;
    private HashMap<UUID, Long> receivedMessages;
    private int percentage;
    private InetSocketAddress reserveParent;
    private ConcurrentLinkedQueue<String> messageStringsList;

    Station(int port, InetSocketAddress parent, int percentage, ConcurrentLinkedQueue<String> messageStringsList) throws SocketException {
        socket = new DatagramSocket(port);
        socket.setSoTimeout(5);
        communicator = new HashMap<>();
        this.parent = parent;
        reserveParent = null;
        this.percentage = percentage;
        this.reserveToSend = null;
        if (parent!=null){
            communicator.put(parent, new NodeInfo());
            byte[] buf = new byte[1];
            buf[0] = (byte) 0;
            communicator.get(parent).systemMessage = new DatagramPacket(buf, buf.length, parent);
        }

        receivedMessages = new HashMap<>();
        this.messageStringsList = messageStringsList;
        msg = null; /**/
    }

    private void sendMessages(){
        Iterator<Map.Entry<InetSocketAddress, NodeInfo>> communicatorIterator = communicator.entrySet().iterator();
        boolean parentChanged = false, reserveToSendDisconnected = false;
        ArrayList<InetSocketAddress> neigbourRemoveList = new ArrayList<>();
        ArrayList<UUID> messageRemoveList = new ArrayList<>();
        while (communicatorIterator.hasNext()) {
            Map.Entry<InetSocketAddress, NodeInfo> communicatorEntry = communicatorIterator.next();

            doSomething(messageRemoveList, communicatorEntry);

            if (communicatorEntry.getValue().lastTimeReceived != -1 && System.currentTimeMillis() - communicatorEntry.getValue().lastTimeReceived > 1700){ /*если нода не отвечала в течение 1.7 сек, считаем что она отключилась*/
                if (parent == communicatorEntry.getKey()){
                    parent = reserveParent;
                    reserveParent = null;
                    parentChanged = true;
                    if (parent!=null) {
                        communicator.put(parent, new NodeInfo());
                        byte[] buf = new byte[1];
                        buf[0] = (byte) 0;
                        communicator.get(parent).systemMessage = new DatagramPacket(buf, buf.length, parent);
                    }
                }
                if (reserveToSend == communicatorEntry.getKey()){
                    reserveToSendDisconnected = true;
                    reserveToSend = null;
                }
                neigbourRemoveList.add(communicatorEntry.getKey());
            }
        }

        for(InetSocketAddress e: neigbourRemoveList){
            communicator.remove(e);
        }
        neigbourRemoveList.clear();

        if (parentChanged){
            if(parent!=null) {

                byte[] bufForSysMsg = constructBufferForSystemMessage(parent);
                for (Map.Entry<InetSocketAddress, NodeInfo> entry : communicator.entrySet()) {
                    if (!entry.getKey().equals(parent)) {
                        entry.getValue().systemMessage = new DatagramPacket(bufForSysMsg, bufForSysMsg.length, entry.getKey());
                    }
                }
            }
        }

        if (reserveToSendDisconnected){
            if (!communicator.isEmpty()){
                communicatorIterator = communicator.entrySet().iterator();
                reserveToSend = communicatorIterator.next().getKey();
                byte[] bufForSysMsg = constructBufferForSystemMessage(reserveToSend);
                if (communicator.size()>1){
                    for(Map.Entry<InetSocketAddress, NodeInfo> entry : communicator.entrySet()){
                        if (!entry.getKey().equals(reserveToSend)){
                            entry.getValue().systemMessage = new DatagramPacket(bufForSysMsg, bufForSysMsg.length, entry.getKey());
                        }
                    }
                }
            }
        }
    }

    private void doSomething(ArrayList<UUID> messageRemoveList, Map.Entry<InetSocketAddress, NodeInfo> communicatorEntry) {
        Iterator<Map.Entry<UUID, DatagramPacket>> messageIterator = communicatorEntry.getValue().message.entrySet().iterator();

        while (messageIterator.hasNext()) {
            try {
                Map.Entry<UUID, DatagramPacket> messageEntry = messageIterator.next();
                socket.send(messageEntry.getValue());
                UUID messageKey = messageEntry.getKey();
                if (communicatorEntry.getValue().firstTimeSent.containsKey(messageKey)){
                    if (System.currentTimeMillis() - communicatorEntry.getValue().firstTimeSent.get(messageKey) > 1500){ /*если сообщение не дошло за 1.5сек, тогда его нужно удалить*/
                        messageRemoveList.add(messageKey);
                    }
                } else {
                    communicatorEntry.getValue().firstTimeSent.put(messageKey, System.currentTimeMillis());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for(UUID e : messageRemoveList){
            communicatorEntry.getValue().message.remove(e);
            communicatorEntry.getValue().firstTimeSent.remove(e); //new detail

        }
        messageRemoveList.clear();
    }

    private void sendSystemMessages(){
        Iterator<Map.Entry<InetSocketAddress, NodeInfo>> entryIterator = communicator.entrySet().iterator();
        while(entryIterator.hasNext()){
            Map.Entry<InetSocketAddress, NodeInfo> communicatorEntry = entryIterator.next();
            try {

                socket.send(communicatorEntry.getValue().systemMessage);

            } catch (IOException e) {
                System.err.println("failed to send system message");
                e.printStackTrace();
            }
        }
    }

    private byte[] constructBufferForSystemMessage(InetSocketAddress address){ /*построение системного сообщения для потомка от предка*/
        byte[] bufForSysMsg = new byte[1 + 1 + address.getAddress().getAddress().length + 2];
        bufForSysMsg[0] = 0;
        bufForSysMsg[1] = (byte) address.getAddress().getAddress().length;
        System.arraycopy(address.getAddress().getAddress(), 0, bufForSysMsg, 2, address.getAddress().getAddress().length);
        bufForSysMsg[2+address.getAddress().getAddress().length] = (byte) address.getPort();
        bufForSysMsg[2+address.getAddress().getAddress().length+1] = (byte) (address.getPort() >> 8);


        return bufForSysMsg;
    }

    private void receiveMessages(){
        byte[] buf = new byte[BUF_SIZE];

        DatagramPacket receivedPack = new DatagramPacket(buf, buf.length);
        byte[] receivedData;
        int randmomNumber = ((int) (Math.random()*100.0));
        if (randmomNumber<percentage)
            return;
        try {
            socket.receive(receivedPack);
            long timeOfReceive = System.currentTimeMillis();
            receivedData = new byte[receivedPack.getLength()];
            System.arraycopy(receivedPack.getData(), 0, receivedData, 0, receivedPack.getLength());

            int messageType = receivedData[0];
            InetSocketAddress senderAddress = (InetSocketAddress) receivedPack.getSocketAddress();
            if (messageType == 0){ /*системное сообщение*/
                if (senderAddress.equals(parent)){ /*родителя*/
                    int numbOfBytesForInetAddress = receivedData[1] & 0xFF;
                    if(numbOfBytesForInetAddress != 0){
                        InetAddress inetAddressOfReserveParent = InetAddress.getByAddress(Arrays.copyOfRange(receivedData, 2, 2 + numbOfBytesForInetAddress /*- 1*/));
                        int portOfReserveParent = receivedData[2 + numbOfBytesForInetAddress+1] & 0xFF;
                        portOfReserveParent = portOfReserveParent<<8;
                        portOfReserveParent = portOfReserveParent | (receivedData[2 + numbOfBytesForInetAddress] & 0xFF);

                        if (reserveParent == null){
                            reserveParent = new InetSocketAddress(inetAddressOfReserveParent, portOfReserveParent);
                        }

                        if(!reserveParent.getAddress().equals(inetAddressOfReserveParent) & reserveParent.getPort() != portOfReserveParent){
                            reserveParent = new InetSocketAddress(inetAddressOfReserveParent, portOfReserveParent);
                        }
                    }
                } else { /*добавление потомка в коммуникатор*/
                    if (!communicator.containsKey(senderAddress)){
                        communicator.put(senderAddress, new NodeInfo());

                        byte[] bufForSysMsg = null;

                        if (parent!=null){
                            bufForSysMsg = constructBufferForSystemMessage(parent);
                        }

                        if (parent==null && reserveToSend!=null){
                            bufForSysMsg = constructBufferForSystemMessage(reserveToSend);
                        }

                        if (parent==null && reserveToSend==null && communicator.size() >1){
                            for(Map.Entry<InetSocketAddress, NodeInfo> entry : communicator.entrySet()){
                                if (entry.getKey() != senderAddress){
                                    reserveToSend = entry.getKey();
                                    bufForSysMsg = constructBufferForSystemMessage(reserveToSend);
                                    break;
                                }
                            }
                        }

                        if (parent==null & reserveToSend==null & communicator.size() == 1) {
                            bufForSysMsg = new byte[2];
                            bufForSysMsg[0] = (byte) 0;
                            bufForSysMsg[1] = (byte) 0;
                        }

                        communicator.get(senderAddress).systemMessage = new DatagramPacket(bufForSysMsg, bufForSysMsg.length, senderAddress);
                    }
                }
            } else if (messageType == 1){ /*нам пришло обычное сообщение, тут мы адресат, соответсвенно нужно будет оповестить о получении сообщения адресанта*/

                byte[] messageInBytes = Arrays.copyOfRange(receivedData, 17,  receivedPack.getLength());
                String message = new String(messageInBytes);
                UUID msgId = UUID.nameUUIDFromBytes(Arrays.copyOfRange(receivedData, 1, 16));

                if (!receivedMessages.containsKey(msgId)) {
                    System.out.println(message);
                    receivedMessages.put(msgId, timeOfReceive);
                    for (Map.Entry<InetSocketAddress, NodeInfo> communicatorEntry : communicator.entrySet()){
                        if (!communicatorEntry.getKey().equals(senderAddress)) {
                            communicatorEntry.getValue().message.put(msgId, new DatagramPacket(receivedData, receivedData.length, communicatorEntry.getKey()));
                        }
                    }
                }

                byte[] acceptMessage = new byte[17];
                acceptMessage[0] = (byte) 2;
                System.arraycopy(receivedData, 1, acceptMessage, 1, 16);

                socket.send(new DatagramPacket(acceptMessage, acceptMessage.length, senderAddress)); /*отправляем подтверждение о получении адресанту*/
            } else if (messageType == 2){ /*адресат оповещает нас о том, что сообщение получено*/
                UUID msgId = UUID.nameUUIDFromBytes(Arrays.copyOfRange(receivedData, 1, 16));
                communicator.get(senderAddress).message.remove(msgId);
                communicator.get(senderAddress).firstTimeSent.remove(msgId);
            }

            ArrayList<UUID> receivedMessagesRemoveList = new ArrayList<>();
            for(Map.Entry<UUID, Long> entry : receivedMessages.entrySet()){
                if (System.currentTimeMillis() - entry.getValue()>2500){ //1100
                    receivedMessagesRemoveList.add(entry.getKey());
                }
            }
            for(UUID e : receivedMessagesRemoveList){
                receivedMessages.remove(e);
            }
            receivedMessagesRemoveList.clear();

            communicator.get(senderAddress).lastTimeReceived = timeOfReceive;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e){
            //e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private void addMessage(){
        while(!messageStringsList.isEmpty()) {
            String curMsg = messageStringsList.poll();
            Integer num = 1;
            UUID msgId = UUID.randomUUID();
            byte[] buf = new byte[1 + 16 + curMsg.getBytes(StandardCharsets.UTF_8).length];
            buf[0] = num.byteValue();
            System.arraycopy(asBytes(msgId), 0, buf, 1, 16);
            System.arraycopy(curMsg.getBytes(StandardCharsets.UTF_8), 0, buf, 1 + 16, curMsg.getBytes(StandardCharsets.UTF_8).length); /*utf-8 кодировку надо*/ /*#fixed*/
            for (Map.Entry<InetSocketAddress, NodeInfo> communicatorEntry : communicator.entrySet()) {
                communicatorEntry.getValue().message.put(msgId, new DatagramPacket(buf, buf.length, communicatorEntry.getKey()));
            }
        }
    }

    @Override
    public void run() {

        while (true){

            sendSystemMessages();
            if(!messageStringsList.isEmpty()) {
                addMessage();
            }
            sendMessages();
            receiveMessages();
        }

    }
}
