import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.UUID;

public class NodeInfo {


    public HashMap<UUID, DatagramPacket> message;
    public HashMap<UUID, Long> firstTimeSent;
    public DatagramPacket systemMessage;
    long lastTimeReceived;
    /*в буфере датаграмы системного сообщения:
     * нулевая ячейка - тип сообщения,
     * 1-16 uuid,
     * 17 - число зарезервированных под ip; если 17ый байт равен нулю, значит нет родителей
     * 18 - ? InetAddress
     * ?+1 - ?? port*/
    NodeInfo(){
        message = new HashMap<>();
        firstTimeSent = new HashMap<>();
        lastTimeReceived = -1;
    }
}