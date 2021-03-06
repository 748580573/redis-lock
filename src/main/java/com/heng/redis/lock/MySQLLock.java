package com.heng.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MySQLLock implements Lock {

    private Logger logger = LoggerFactory.getLogger(MySQLLock.class);

    private DataSource dataSource;

    private String table = "lock";

    String resource = "test";

    public MySQLLock(DataSource dataSource){
        this.dataSource = dataSource;
    }

    @Override
    public void lock() {
        try {
            String thread_id = String.valueOf(Thread.currentThread().getId());
            String host_ip = getSystemLocalIp().getHostAddress();
            Connection connection = dataSource.getConnection();
            String sql = null;
            PreparedStatement preparedStatement = null;
            if (isLockByCurrentThread(connection)){
                sql = "update `lock` set entry_count = entry_count + 1 where thread_id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1,thread_id);
            }else {
                sql = "insert into `lock`(resource,thread_id,entry_count,host_ip) value(?,?,?,?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1,resource);
                preparedStatement.setString(2,thread_id);
                preparedStatement.setInt(3,1);
                preparedStatement.setString(4,host_ip);
            }

            int updateCount = preparedStatement.executeUpdate();
            if (updateCount <= 0){
                throw new Exception("lock fail");
            }

        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        try {
            Connection connection = dataSource.getConnection();
            String thread_id = String.valueOf(Thread.currentThread().getId());
            String sql = "update `lock` set entry_count= entry_count -  1 where thread_id = ? and resource = ? and entry_count > 0";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,thread_id);
            preparedStatement.setString(2,resource);
                int updateCount = preparedStatement.executeUpdate();
            if (updateCount <= 0){
                String deleteSQL = "delete from `lock` where thread_id = ? and resource = ?";
                PreparedStatement deletePreparedStatement = connection.prepareStatement(deleteSQL);
                deletePreparedStatement.setString(1,thread_id);
                deletePreparedStatement.setString(2,resource);
                deletePreparedStatement.executeUpdate();
                throw new Exception("current wasn't locked");
            }
        } catch (Exception throwables) {
            throwables.printStackTrace();
            throw new RuntimeException(throwables.getMessage());
        }

    }

    private boolean isLockByCurrentThread(Connection connection) throws SQLException {
        String sql = "select * from `lock` where resource = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,resource);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()){
            return true;
        }
        return false;
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    public InetAddress getSystemLocalIp() throws UnknownHostException {
        InetAddress inet=null;
        String osname=getSystemOSName();
        try {
            //??????window??????
            if(osname.contains("Windows")){
                inet=getWinLocalIp();
                //??????linux??????
            }else if(osname.equalsIgnoreCase("Linux")){
                inet=getUnixLocalIp();
            }
            if(null==inet){
                throw new UnknownHostException("?????????ip????????????");
            }
        }catch (SocketException e) {
            logger.error("????????????ip??????"+e.getMessage());
            throw new UnknownHostException("????????????ip??????"+e.getMessage());
        }
        return inet;
    }

    /**
     * ??????FTP?????????????????????
     * @return
     */
    public String getSystemOSName() {
        //?????????????????????
        Properties props=System.getProperties();
        //??????????????????
        String osname=props.getProperty("os.name");
        if(logger.isDebugEnabled()){
            logger.info("the ftp client system os Name "+osname);
        }
        return osname;
    }

    private static InetAddress getWinLocalIp() throws UnknownHostException{
        InetAddress inet = InetAddress.getLocalHost();
        System.out.println("?????????ip=" + inet.getHostAddress());
        return inet;
    }

    private static InetAddress getUnixLocalIp() throws SocketException {
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip = null;
        while(netInterfaces.hasMoreElements())
        {
            NetworkInterface ni= (NetworkInterface)netInterfaces.nextElement();
            ip=(InetAddress) ni.getInetAddresses().nextElement();
            if( !ip.isSiteLocalAddress()
                    && !ip.isLoopbackAddress()
                    && ip.getHostAddress().indexOf(":")==-1)
            {
                return ip;
            }
            else
            {
                ip=null;
            }
        }
        return null;
    }
}
