import React from 'react';
import { Tabs, Collapse, Table, Progress } from 'antd';
import { usePerfData } from './UsePerfData';
import { useHistoryData } from './UseHistoryData';
import { Line, Column } from '@ant-design/charts';
import { useTranslation } from 'react-i18next';

const { TabPane } = Tabs;
const { Panel } = Collapse;

const cardStyle = {
  background: '#fff',
  borderRadius: 8,
  padding: 24,
  boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  border: '1px solid #e5e6eb',
  minWidth: 0,
};

const CardTitle: React.FC<{ title: string }> = ({ title }) => (
  <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
    <div style={{ width: 5, height: 20, background: '#1677ff', borderRadius: 2, marginRight: 10 }} />
    <span style={{ fontSize: 16, fontWeight: 600, color: '#222' }}>{title}</span>
  </div>
);

// 时间转换
function formatTime(seconds: number, t?: (key: string, options?: any) => string): string {
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (!t) {
    return `${d ? d + '天' : ''}${h ? h + '小时' : ''}${m ? m + '分' : ''}${s ? s + '秒' : ''}`;
  }
  return `${d ? d + t('day') : ''}${h ? h + t('hour') : ''}${m ? m + t('minute') : ''}${s ? s + t('second') : ''}`;
}

// 单位自适应
function getBestUnit(bytes: number) {
  if (bytes < 1024) return { unit: 'B', factor: 1 };
  if (bytes < 1024 * 1024) return { unit: 'KB', factor: 1024 };
  if (bytes < 1024 * 1024 * 1024) return { unit: 'MB', factor: 1024 * 1024 };
  return { unit: 'GB', factor: 1024 * 1024 * 1024 };
}
function formatBytesAuto(bytes: number): string {
  const { unit, factor } = getBestUnit(bytes);
  return (bytes / factor).toFixed(2) + ' ' + unit;
}

// 进度条格式化函数
const dashboardFormat = (percent?: number) => <span style={{ color: '#222' }}>{percent ?? 0}%</span>;
const tpsDashboardFormat = (tps?: number) => <span style={{ color: '#222' }}>{tps ?? 0}</span>;

// G2Plot 主题
const chartTheme = undefined;

// 表格主题
const tableProps = {
  style: {
    background: '#fff',
    color: '#333',
  },
  pagination: false as const
};

// 内存相关折线图统一用MB
function toMB(val: number) { return val / (1024 * 1024); }
// 网络相关折线图统一用KB
function toKB(val: number) { return val / 1024; }

const Performance: React.FC = () => {
  const { t } = useTranslation();
  const { data: perfData, enabled } = usePerfData();

  // 服务器tab数据
  const server = perfData?.server || {};
  const tps = server.tps || { '1m': 20, '5m': 20, '15m': 20 };
  const onlinePlayers = server.worlds?.reduce((sum: number, w: any) => sum + (w.players || 0), 0) || 0;
  const totalEntities = server.total_entities || 0;
  const livingEntities = server.living_entities || 0;
  const loadedChunks = server.loaded_chunks || 0;

  // 历史数据
  const tps1m = tps['1m'] || 20;
  const tpsHistory = useHistoryData(tps1m);
  const onlineHistory = useHistoryData(onlinePlayers);
  const entityHistory = useHistoryData(totalEntities);
  const livingEntityHistory = useHistoryData(livingEntities);
  const chunkHistory = useHistoryData(loadedChunks);

  // Java Tab数据
  const java = perfData?.java || {};
  const heapUsed = java.memory?.heap?.used || 0;
  const heapMax = java.memory?.heap?.max || 0;
  const nonHeapUsed = java.memory?.non_heap?.used || 0;
  const threadCount = java.threads?.count || 0;
  const daemonThreadCount = java.threads?.daemon_count || 0;
  const peakThreadCount = java.threads?.peak_count || 0;
  const heapHistory = useHistoryData(heapUsed);
  const nonHeapHistory = useHistoryData(nonHeapUsed);
  const threadHistory = useHistoryData(threadCount);
  const daemonThreadHistory = useHistoryData(daemonThreadCount);
  const javaMemory = java.memory?.total?.max || 0;
  const javaUsedMemory = java.memory?.total?.used || 0;
  const javaMemoryHistory = useHistoryData(javaUsedMemory);

  // 系统Tab数据
  const system = perfData?.system || {};
  const cpu = system.cpu || {};
  const memory = system.memory || {};
  const disk = system.disk || {};
  const network = system.network || {};
  const os = system.os || {};

  // CPU历史
  const cpuUsage = cpu.usage_percent || 0;
  const cpuHistory = useHistoryData(cpuUsage);
  // 内存历史
  const memUsage = memory.usage_percent || 0;
  const memHistory = useHistoryData(memUsage);
  // 网络历史
  const netSend = network.bytes_sent_per_sec || 0;
  const netRecv = network.bytes_recv_per_sec || 0;
  const netSendHistory = useHistoryData(netSend);
  const netRecvHistory = useHistoryData(netRecv);
  const socketConn = network.socket_connection_count || 0;
  const socketConnHistory = useHistoryData(socketConn);

  // JVM信息
  const jvm = java.jvm || {};
  const jvmVersion = (jvm.version || '-') + (jvm.vendor ? ' by ' + jvm.vendor : '');
  const jvmUptime = java.uptime || 0;

  // 基本信息卡片数据
  const osInfo = perfData?.system?.os || {};

  // 磁盘Tab数据
  const diskStores = Array.isArray(disk.stores) ? disk.stores : [];

  // 网络包速率历史
  const packetsSentPerSec = network.packets_sent_per_sec || 0;
  const packetsRecvPerSec = network.packets_recv_per_sec || 0;
  const packetsSentHistory = useHistoryData(packetsSentPerSec);
  const packetsRecvHistory = useHistoryData(packetsRecvPerSec);

  if (enabled === null) {
    return (
      <div style={{ color: '#333', textAlign: 'center', padding: 64 }}>
        {t('loading', 'Loading...')}
      </div>
    );
  }

  if (enabled === false) {
    return (
      <div style={{
        color: '#333',
        textAlign: 'center',
        padding: 64,
        background: '#fff',
        borderRadius: 8,
        margin: 24,
        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        border: '1px solid #e5e6eb'
      }}>
        <h2 style={{ color: '#1890ff', marginBottom: 16 }}>
          {t('performance.disabled.title', 'Performance Monitor Disabled')}
        </h2>
        <p style={{ fontSize: 16, marginBottom: 24 }}>
          {t('performance.disabled.description', 'Performance monitoring is currently disabled. Enable it in the configuration to view performance data.')}
        </p>
      </div>
    );
  }

  if (!perfData) {
    return (
      <div style={{ color: '#333', textAlign: 'center', padding: 64 }}>
        {t('loading', 'Loading...')}
      </div>
    );
  }

  // 主面板背景色
  const bgColor = '#f0f2f5';
  // areaStyle配置
  const lineAreaStyle = { area: { style: { fill: 'rgba(22,119,255,0.15)' } } };
  const axisStyleObj = {
    label: { style: { fill: '#666', fontSize: 12 } },
    line: { style: { stroke: '#ccc', lineWidth: 1 } },
    tickLine: { style: { stroke: '#ccc', lineWidth: 1 } },
    title: { style: { fill: '#666', fontSize: 12 } },
  };
  const legendStyleObj = {
    itemName: { style: { fill: '#222', fontSize: 12 } },
    marker: { style: { fill: '#222' } },
  };
  return (
    <div style={{ background: bgColor, overflowY: 'auto', minHeight: '100vh', width: '100%' }}>
      <div style={{ width: '100%', padding: '16px 32px 24px 32px' }}>
        {/* 基本信息卡片 */}
        <div style={{ ...cardStyle, marginBottom: 24, display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ width: 5, height: 24, background: '#1677ff', borderRadius: 2, marginRight: 12 }} />
            <span style={{ fontSize: 18, fontWeight: 'bold', color: '#222' }}>{t('basicInfo')}</span>
          </div>
          <div style={{ display: 'flex', width: '100%', gap: 0, fontSize: 16, color: '#333' }}>
            <div style={{ flex: 1, textAlign: 'center' }}>{t('osVersion')}：<span style={{ fontWeight: 500 }}>{osInfo.os_version || '-'}</span></div>
            <div style={{ flex: 1, textAlign: 'center' }}>{t('osUptime')}：<span style={{ fontWeight: 500 }}>{formatTime(osInfo.uptime || 0, t)}</span></div>
            <div style={{ flex: 1, textAlign: 'center' }}>Java {t('version')}：<span style={{ fontWeight: 500 }}>{jvmVersion}</span></div>
            <div style={{ flex: 1, textAlign: 'center' }}>JVM {t('uptime')}：<span style={{ fontWeight: 500 }}>{formatTime(jvmUptime / 1000, t)}</span></div>
          </div>
        </div>
        {/* 概览卡片区 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: 24,
          marginBottom: 24,
          width: '100%',
        }}>
          <div style={cardStyle}>
            <CardTitle title='TPS' />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <Progress
                type='dashboard'
                percent={Math.round((tps['1m'] / 20) * 100)}
                format={() => tpsDashboardFormat(tps['1m'])}
                strokeColor='#1677ff'
                size={120}
              />
            </div>
          </div>
          <div style={cardStyle}>
            <CardTitle title='CPU' />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <Progress
                type='dashboard'
                percent={Math.round(cpuUsage)}
                format={dashboardFormat}
                strokeColor='#1677ff'
                size={120}
              />
            </div>
          </div>
          <div style={cardStyle}>
            <CardTitle title={t('memory')} />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <Progress
                type='dashboard'
                percent={memory.total ? Math.round((memory.used / memory.total) * 100) : 0}
                format={dashboardFormat}
                strokeColor='#1677ff'
                size={120}
              />
              <div style={{ color: '#333', fontSize: 14, marginTop: 4 }}>
                {formatBytesAuto(memory.used || 0)} / {formatBytesAuto(memory.total || 0)}
              </div>
            </div>
          </div>
          <div style={cardStyle}>
            <CardTitle title={t('disk')} />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <Progress
                type='dashboard'
                percent={disk.usage_percent ? Math.round(disk.usage_percent) : 0}
                format={dashboardFormat}
                strokeColor='#1677ff'
                size={120}
              />
            </div>
          </div>
          <div style={cardStyle}>
            <CardTitle title={t('network')} />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: 64 }}>
              <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                {t('send')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{formatBytesAuto(netSend)}/s</span>
              </div>
              <div style={{ fontSize: 20, color: '#333' }}>
                {t('recv')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{formatBytesAuto(netRecv)}/s</span>
              </div>
            </div>
          </div>
        </div>
        {/* 详细数据区 */}
        <div style={{ ...cardStyle, minHeight: 300 }}>
          <Tabs defaultActiveKey='server' style={{ marginTop: -16 }}>
            <TabPane tab={t('server')} key='server'>
              {/* 服务器Tab内容 */}
              <div style={{ display: 'flex', gap: 24, marginBottom: 24 }}>
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('tps_1m_5m_15m')} />
                  <div style={{ fontSize: 24, color: '#1677ff', textAlign: 'center', fontWeight: 600 }}>
                    {tps['1m']} / {tps['5m']} / {tps['15m']}
                  </div>
                </div>
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('onlinePlayers')} />
                  <div style={{ fontSize: 24, color: '#1677ff', textAlign: 'center', fontWeight: 600 }}>
                    {onlinePlayers}
                  </div>
                </div>
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('entities')} />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: 64 }}>
                    <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                      {t('totalEntities')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{totalEntities}</span>
                    </div>
                    <div style={{ fontSize: 20, color: '#333' }}>
                      {t('livingEntities')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{livingEntities}</span>
                    </div>
                  </div>
                </div>
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('chunks')} />
                  <div style={{ fontSize: 24, color: '#1677ff', textAlign: 'center', fontWeight: 600 }}>
                    {loadedChunks}
                  </div>
                </div>
              </div>
              {/* 折线图区块 */}
              <Collapse ghost>
                <Panel header={<span style={{ color: '#222' }}>{t('tpsHistory')}</span>} key='tps'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line data={tpsHistory.map((v, i) => ({ time: i, value: v, type: 'TPS' }))} xField='time' yField='value' seriesField='type' height={180} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </Panel>
                <Panel header={<span style={{ color: '#222' }}>{t('onlineHistory')}</span>} key='online'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line data={onlineHistory.map((v, i) => ({ time: i, value: v, type: t('onlinePlayers') }))} xField='time' yField='value' seriesField='type' height={180} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </Panel>
                <Panel header={<span style={{ color: '#222' }}>{t('entityHistory')}</span>} key='entity'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line
                      data={entityHistory.map((v, i) => ({ time: i, value: v, type: t('totalEntities') })).concat(
                        livingEntityHistory.map((v, i) => ({ time: i, value: v, type: t('livingEntities') }))
                      )}
                      xField='time'
                      yField='value'
                      seriesField='type'
                      height={180}
                      smooth
                      autoFit
                      color={['#1677ff', '#52c41a']}
                      xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme}
                    />
                  </div>
                </Panel>
                <Panel header={<span style={{ color: '#222' }}>{t('chunkHistory')}</span>} key='chunk'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line data={chunkHistory.map((v, i) => ({ time: i, value: v, type: t('chunks') }))} xField='time' yField='value' seriesField='type' height={180} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </Panel>
              </Collapse>
              {/* 世界信息表格 */}
              <div style={{ ...cardStyle }}>
                <CardTitle title={t('worldInfo')} />
                <Table
                  size='small'
                  rowKey='name'
                  columns={[
                    { title: t('worldName'), dataIndex: 'name', key: 'name' },
                    { title: t('type'), dataIndex: 'type', key: 'type' },
                    { title: t('entities'), dataIndex: 'entities', key: 'entities' },
                    { title: t('loadedChunks'), dataIndex: 'loaded_chunks', key: 'loaded_chunks' },
                    { title: t('players'), dataIndex: 'players', key: 'players' },
                    { title: t('time'), dataIndex: 'time', key: 'time' },
                  ]}
                  dataSource={Array.isArray(server.worlds) ? server.worlds.map((w: any) => ({
                    ...w,
                    time: w.time !== undefined ? w.time : '-',
                  })) : []}
                  {...tableProps}
                />
              </div>
            </TabPane>
            <TabPane tab={t('java')} key='java'>
              {/* Java Tab内容 */}
              <div style={{ display: 'flex', gap: 24, marginBottom: 24 }}>
                {/* 总内存使用卡片 */}
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('javaMemoryUsage')} />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                    <Progress
                      type='dashboard'
                      percent={javaMemory ? Math.round((javaUsedMemory / javaMemory) * 100) : 0}
                      format={dashboardFormat}
                      strokeColor='#1677ff'
                      size={120}
                    />
                    <div style={{ color: '#333', fontSize: 14, marginTop: 4 }}>
                      {formatBytesAuto(javaUsedMemory)} / {formatBytesAuto(javaMemory)}
                    </div>
                  </div>
                </div>
                {/* 堆内存使用卡片 */}
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('heapMemoryUsage')} />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                    <Progress
                      type='dashboard'
                      percent={typeof heapMax === 'number' && heapMax > 0 ? Math.round((heapUsed / heapMax) * 100) : 0}
                      format={dashboardFormat}
                      strokeColor='#1677ff'
                      size={120}
                    />
                    <div style={{ color: '#333', fontSize: 14, marginTop: 4 }}>
                      {formatBytesAuto(heapUsed)} / {formatBytesAuto(heapMax)}
                    </div>
                  </div>
                </div>
                {/* 线程数卡片 */}
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('threadCount')} />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 100, paddingTop: 8 }}>
                    <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                      {t('threadCount')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{threadCount}</span>
                    </div>
                    <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                      {t('daemonThreadCount')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{daemonThreadCount}</span>
                    </div>
                    <div style={{ fontSize: 20, color: '#333' }}>
                      {t('peakThreadCount')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{peakThreadCount}</span>
                    </div>
                  </div>
                </div>
                {/* JVM信息卡片 */}
                <div style={{ flex: 1, ...cardStyle }}>
                  <CardTitle title={t('jvmInfo')} />
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 100, paddingTop: 8 }}>
                    <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                      {t('jvmVersion')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{java.jvm?.version || '-'}</span>
                    </div>
                    <div style={{ fontSize: 20, color: '#333', marginBottom: 4 }}>
                      {t('jvmVendor')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{java.jvm?.vendor || '-'}</span>
                    </div>
                    <div style={{ fontSize: 20, color: '#333' }}>
                      {t('jvmUptime')} <span style={{ color: '#1677ff', fontWeight: 600 }}>{formatTime(java.uptime / 1000, t)}</span>
                    </div>
                  </div>
                </div>
              </div>
              {/* 折线图区块 */}
              <Collapse ghost>
                {/* 总内存历史曲线 */}
                <Panel header={<span style={{ color: '#222' }}>{t('javaMemoryHistory')}</span>} key='javaMemory'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line
                      data={javaMemoryHistory.map((v, i) => ({ time: i, value: Number(toMB(v).toFixed(2)), type: t('javaMemory') }))}
                      xField='time'
                      yField='value'
                      seriesField='type'
                      height={180}
                      smooth
                      autoFit
                      color='#1677ff'
                      xAxis={axisStyleObj}
                      yAxis={{ ...axisStyleObj, label: { ...axisStyleObj.label, formatter: (v: string | number) => `${v} MB` } }}
                      legend={legendStyleObj}
                      {...lineAreaStyle}
                      theme={chartTheme}
                    />
                  </div>
                </Panel>
                {/* 堆内存历史曲线 */}
                <Panel header={<span style={{ color: '#222' }}>{t('heapHistory')}</span>} key='heap'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line
                      data={heapHistory.map((v, i) => ({ time: i, value: Number(toMB(v).toFixed(2)), type: t('heapMemory') }))}
                      xField='time'
                      yField='value'
                      seriesField='type'
                      height={180}
                      smooth
                      autoFit
                      color='#1677ff'
                      xAxis={axisStyleObj}
                      yAxis={{ ...axisStyleObj, label: { ...axisStyleObj.label, formatter: (v: string | number) => `${v} MB` } }}
                      legend={legendStyleObj}
                      {...lineAreaStyle}
                      theme={chartTheme}
                    />
                  </div>
                </Panel>
                {/* 非堆内存历史曲线 */}
                <Panel header={<span style={{ color: '#222' }}>{t('nonHeapHistory')}</span>} key='nonheap'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line
                      data={nonHeapHistory.map((v, i) => ({ time: i, value: Number(toMB(v).toFixed(2)), type: t('nonHeapMemory') }))}
                      xField='time'
                      yField='value'
                      seriesField='type'
                      height={180}
                      smooth
                      autoFit
                      color='#1677ff'
                      xAxis={axisStyleObj}
                      yAxis={{ ...axisStyleObj, label: { ...axisStyleObj.label, formatter: (v: string | number) => `${v} MB` } }}
                      legend={legendStyleObj}
                      {...lineAreaStyle}
                      theme={chartTheme}
                    />
                  </div>
                </Panel>
                <Panel header={<span style={{ color: '#222' }}>{t('threadHistory')}</span>} key='thread'>
                  <div style={{ ...cardStyle, marginBottom: 24 }}>
                    <Line
                      data={threadHistory.map((v, i) => ({ time: i, value: v, type: t('threadCount') })).concat(
                        daemonThreadHistory.map((v, i) => ({ time: i, value: v, type: t('daemonThreadCount') }))
                      )}
                      xField='time'
                      yField='value'
                      seriesField='type'
                      height={180}
                      smooth
                      autoFit
                      color={['#1677ff', '#52c41a']}
                      xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme}
                    />
                  </div>
                </Panel>
              </Collapse>
              {/* GC信息表格 */}
              <div style={{ ...cardStyle, marginBottom: 24 }}>
                <CardTitle title={t('gcInfo')} />
                <Table
                  size='small'
                  rowKey='name'
                  columns={[
                    { title: t('gcName'), dataIndex: 'name', key: 'name' },
                    { title: t('gcCount'), dataIndex: 'collection_count', key: 'collection_count' },
                    { title: t('gcTime'), dataIndex: 'collection_time', key: 'collection_time' },
                  ]}
                  dataSource={(() => {
                    const gc = java.gc || {};
                    return Object.keys(gc).map(name => ({
                      name,
                      collection_count: gc[name]?.collection_count ?? '-',
                      collection_time: gc[name]?.collection_time ?? '-',
                    }));
                  })()}
                  {...tableProps}
                />
              </div>
            </TabPane>
            <TabPane tab={t('system')} key='system'>
              {/* 系统信息区块 */}
              <div style={{ ...cardStyle, marginBottom: 24, display: 'flex', flexDirection: 'column', position: 'relative' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
                  <div style={{ width: 5, height: 24, background: '#1677ff', borderRadius: 2, marginRight: 12 }} />
                  <span style={{ fontSize: 18, fontWeight: 'bold', color: '#222' }}>{t('systemInfo')}</span>
                </div>
                <div style={{ display: 'flex', width: '100%', gap: 0, fontSize: 16, color: '#333' }}>
                  <div style={{ flex: 1, textAlign: 'center' }}>{t('osVersion')}：<span style={{ fontWeight: 500 }}>{os.os_version || '-'}</span></div>
                  <div style={{ flex: 1, textAlign: 'center' }}>{t('osUptime')}：<span style={{ fontWeight: 500 }}>{formatTime(os.uptime || 0, t)}</span></div>
                  <div style={{ flex: 1, textAlign: 'center' }}>{t('processCount')}：<span style={{ fontWeight: 500 }}>{os.process_count || '-'}</span></div>
                  <div style={{ flex: 1, textAlign: 'center' }}>{t('threadCount')}：<span style={{ fontWeight: 500 }}>{os.thread_count || '-'}</span></div>
                </div>
              </div>
              {/* CPU信息区块 */}
              <div style={{ ...cardStyle, marginBottom: 24, minHeight: 340 }}>
                <CardTitle title={t('cpuInfo')} />
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('cpuName')}：{cpu.name || '-'}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('physicalCores')}：{cpu.physical_cpu_count || '-'}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('logicalCores')}：{cpu.logical_cpu_count || '-'}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('totalUsage')}：{cpuUsage}%</div>
                  <div style={{ flex: 1, height: 120, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb' }}>
                    <Line data={cpuHistory.map((v, i) => ({ time: i, value: v, type: t('cpuUsage') }))} xField='time' yField='value' seriesField='type' height={120} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
                <div style={{ marginTop: 16, color: '#333' }}>{t('perCoreUsage')}：</div>
                <div style={{ height: 240, display: 'flex', alignItems: 'center', color: '#aaa' }}>
                  <Column
                    data={Array.isArray(cpu.per_core_usage) ? cpu.per_core_usage.map((v: number, i: number) => ({ core: t('core', { number: i + 1 }), value: v })) : []}
                    xField='core'
                    yField='value'
                    height={240}
                    color='#1677ff'
                    autoFit
                    label={{ position: 'top' }}
                    xAxis={axisStyleObj}
                    yAxis={axisStyleObj}
                    legend={legendStyleObj}
                    theme={chartTheme}
                  />
                </div>
              </div>
              {/* 内存信息区块 */}
              <div style={{ ...cardStyle, marginBottom: 24 }}>
                <CardTitle title={t('memoryInfo')} />
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 8 }}>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('javaMemory')}：{formatBytesAuto(memory.total || 0)}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('used')}：{formatBytesAuto(memory.used || 0)}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('available')}：{formatBytesAuto(memory.available || 0)}</div>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('virtualMemory')}：{formatBytesAuto(memory.virtual_memory?.total || 0)}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('used')}：{formatBytesAuto(memory.virtual_memory?.used || 0)}</div>
                  <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('available')}：{formatBytesAuto(memory.virtual_memory?.available || 0)}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('usage')}：{memUsage}%</div>
                  <div style={{ flex: 1, height: 120, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb' }}>
                    <Line data={memHistory.map((v, i) => ({ time: i, value: v, type: t('memoryUsage') }))} xField='time' yField='value' seriesField='type' height={120} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
              </div>
              {/* 磁盘信息区块 */}
              <div style={{ ...cardStyle, marginBottom: 24 }}>
                <CardTitle title={t('diskInfo')} />
                <div style={{ marginBottom: 16 }}>
                  <Tabs defaultActiveKey='total' type='card'>
                    <TabPane tab={t('overview')} key='total'>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                        <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalSpace')}：{formatBytesAuto(disk.total_space || 0)}</div>
                        <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('used')}：{formatBytesAuto(disk.used_space || 0)}</div>
                        <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('free')}：{formatBytesAuto(disk.free_space || 0)}</div>
                        <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('usage')}：{disk.usage_percent ? disk.usage_percent.toFixed(2) : 0}%</div>
                      </div>
                    </TabPane>
                    {diskStores.map((store: any, idx: number) => (
                      <TabPane tab={store.name} key={store.name || idx}>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                          <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalSpace')}：{formatBytesAuto(store.total || 0)}</div>
                          <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('used')}：{formatBytesAuto(store.used || 0)}</div>
                          <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('free')}：{formatBytesAuto(store.free || 0)}</div>
                          <div style={{ flex: 1, minWidth: 120, color: '#333' }}>{t('usage')}：{store.usage_percent ? store.usage_percent.toFixed(2) : 0}%</div>
                        </div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                          <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('readSpeed')}：{formatBytesAuto(store.read_bytes_per_sec || 0)}/s</div>
                          <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('writeSpeed')}：{formatBytesAuto(store.write_bytes_per_sec || 0)}/s</div>
                        </div>
                      </TabPane>
                    ))}
                  </Tabs>
                </div>
              </div>
              {/* 网络信息区块 */}
              <div style={{ ...cardStyle }}>
                <CardTitle title={t('networkInfo')} />
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, marginBottom: 16 }}>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalSent')}：{formatBytesAuto(network.total_bytes_sent || 0)}</div>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalRecv')}：{formatBytesAuto(network.total_bytes_recv || 0)}</div>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalPacketsSent')}：{network.total_packets_sent || 0}</div>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('totalPacketsRecv')}：{network.total_packets_recv || 0}</div>
                  <div style={{ flex: 1, minWidth: 180, color: '#333' }}>{t('socketConn')}：{network.socket_connection_count || 0}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32, marginBottom: 8 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('send')}：{formatBytesAuto(netSend)}/s</div>
                  <div style={{ flex: 1, height: 160, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb', minHeight: 160 }}>
                    <Line data={netSendHistory.map((v, i) => ({ time: i, value: Number(toKB(v).toFixed(2)), type: t('sendPerSec') }))} xField='time' yField='value' seriesField='type' height={160} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={{ ...axisStyleObj, label: { ...axisStyleObj.label, formatter: (v: string | number) => `${v} KB` } }} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32, marginBottom: 8 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('recv')}：{formatBytesAuto(netRecv)}/s</div>
                  <div style={{ flex: 1, height: 160, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb', minHeight: 160 }}>
                    <Line data={netRecvHistory.map((v, i) => ({ time: i, value: Number(toKB(v).toFixed(2)), type: t('recvPerSec') }))} xField='time' yField='value' seriesField='type' height={160} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={{ ...axisStyleObj, label: { ...axisStyleObj.label, formatter: (v: string | number) => `${v} KB` } }} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32, marginBottom: 8 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('packetsSentPerSec')}：{packetsSentPerSec}</div>
                  <div style={{ flex: 1, height: 160, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb', minHeight: 160 }}>
                    <Line data={packetsSentHistory.map((v, i) => ({ time: i, value: v, type: t('packetsSentPerSec') }))} xField='time' yField='value' seriesField='type' height={160} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32, marginBottom: 8 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('packetsRecvPerSec')}：{packetsRecvPerSec}</div>
                  <div style={{ flex: 1, height: 160, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb', minHeight: 160 }}>
                    <Line data={packetsRecvHistory.map((v, i) => ({ time: i, value: v, type: t('packetsRecvPerSec') }))} xField='time' yField='value' seriesField='type' height={160} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 32, marginBottom: 8 }}>
                  <div style={{ minWidth: 180, color: '#333' }}>{t('socketConn')}</div>
                  <div style={{ flex: 1, height: 160, display: 'flex', alignItems: 'center', color: '#aaa', borderLeft: '1px solid #e5e6eb', minHeight: 160 }}>
                    <Line data={socketConnHistory.map((v, i) => ({ time: i, value: v, type: t('socketConn') }))} xField='time' yField='value' seriesField='type' height={160} smooth autoFit color='#1677ff' xAxis={axisStyleObj} yAxis={axisStyleObj} legend={legendStyleObj} {...lineAreaStyle} theme={chartTheme} />
                  </div>
                </div>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </div>
    </div>
  );
};

export default Performance; 