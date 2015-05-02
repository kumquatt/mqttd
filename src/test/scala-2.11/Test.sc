import java.net.{InetAddress, NetworkInterface}

InetAddress.getLocalHost.getHostAddress

val en = NetworkInterface.getNetworkInterfaces();
while (en.hasMoreElements()) {
  val ni = en.nextElement()
  val ee = ni.getInetAddresses();
  while (ee.hasMoreElements()) {
    val ia = ee.nextElement();
    System.out.println(ia.getHostAddress());
  }
}



InetAddress.getAllByName(InetAddress.getLocalHost().getCanonicalHostName()) match {
  case null => "127.0.0.1"
  case Array() => "127.0.0.1"
  case hostNames: Array[InetAddress] => hostNames.filterNot(x => x.getHostAddress.equals("127.0.0.1")) match {
    case Array() => "127.0.0.1"
    case other: Array[InetAddress] => other(0).getHostAddress
  }
}


