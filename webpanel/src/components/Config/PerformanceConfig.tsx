import React, { useEffect, useState } from 'react';
import { Form, Switch, Button, Card, Space, Typography, Alert, InputNumber } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title, Text } = Typography;

interface PerformanceConfigProps {
  config: any;
  onSave: (data: any) => void;
  loading: boolean;
}

const PerformanceConfig: React.FC<PerformanceConfigProps> = ({ config, onSave, loading }) => {
  const { t } = useTranslation();

  // 使用受控组件模式
  const [enabled, setEnabled] = useState(true);
  const [serverUpdateInterval, setServerUpdateInterval] = useState(20);
  const [systemUpdateInterval, setSystemUpdateInterval] = useState(20);

  // 同步config变化到state
  useEffect(() => {
    if (config && typeof config === 'object') {
      if ('enabled' in config) {
        setEnabled(config.enabled);
      }
      if ('server_update_interval' in config) {
        setServerUpdateInterval(config.server_update_interval);
      }
      if ('system_update_interval' in config) {
        setSystemUpdateInterval(config.system_update_interval);
      }
    }
  }, [config]);

  const handleSubmit = async () => {
    const values = {
      enabled,
      server_update_interval: serverUpdateInterval,
      system_update_interval: systemUpdateInterval,
    };
    await onSave(values);
  };

  const cardStyle = {
    background: '#fff',
    borderRadius: 8,
    padding: 24,
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #e5e6eb',
    marginBottom: 16,
  };

  return (
    <div>
      <Alert
        message={t('config.performanceConfig.description', 'Performance Monitor Configuration')}
        description={t('config.performanceConfig.descriptionDetail',
          'Configure performance monitoring settings. Disabling performance monitoring reduces system resource usage but you will lose access to system performance data.'
        )}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {!enabled && (
        <Alert
          message={t('config.performanceConfig.disabledTitle', 'Performance Monitor Disabled')}
          description={t('config.performanceConfig.disabledDesc',
            'Performance monitoring is currently disabled. System performance data will not be available until you enable it.'
          )}
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Form layout="vertical">
        <Card style={cardStyle}>
          <Title level={4}>{t('config.performanceConfig.basic', 'Basic Settings')}</Title>
          <Form.Item
            label={t('config.performanceConfig.enabled', 'Enable Performance Monitor')}
          >
            <Switch
              checked={enabled}
              onChange={setEnabled}
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.performanceConfig.intervals', 'Update Intervals')}</Title>

          <Form.Item
            label={t('config.performanceConfig.serverInterval', 'Server Data Update Interval (ticks)')}
          >
            <InputNumber
              value={serverUpdateInterval}
              onChange={(value) => setServerUpdateInterval(value || 20)}
              min={1}
              max={1200}
              disabled={!enabled}
              style={{ width: '100%' }}
              placeholder={t('config.performanceConfig.serverIntervalPlaceholder', 'Default: 20 (1 second)')}
            />
          </Form.Item>

          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            {t('config.performanceConfig.serverIntervalDesc',
              'How often to update server data (TPS, entities, chunks, etc.). 20 ticks = 1 second. Lower values provide more real-time data but use more CPU.'
            )}
          </Text>

          <Form.Item
            label={t('config.performanceConfig.systemInterval', 'System Data Update Interval (ticks)')}
          >
            <InputNumber
              value={systemUpdateInterval}
              onChange={(value) => setSystemUpdateInterval(value || 20)}
              min={1}
              max={1200}
              disabled={!enabled}
              style={{ width: '100%' }}
              placeholder={t('config.performanceConfig.systemIntervalPlaceholder', 'Default: 20 (1 second)')}
            />
          </Form.Item>

          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            {t('config.performanceConfig.systemIntervalDesc',
              'How often to update system data (CPU, memory, disk, network). 20 ticks = 1 second. Lower values provide more real-time data but use more CPU.'
            )}
          </Text>
        </Card>

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Space>
            <Button
              type="primary"
              onClick={handleSubmit}
              icon={<SaveOutlined />}
              loading={loading}
              size="large"
            >
              {t('config.performanceConfig.save', 'Save')}
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  );
};

export default PerformanceConfig;
