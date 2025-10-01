import React, { useEffect, useState } from 'react';
import { Form, Input, Switch, InputNumber, Button, Card, Space, Typography, Alert } from 'antd';
import { SaveOutlined, DatabaseOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;

interface MySQLConfigProps {
  config: any;
  onSave: (data: any) => void;
  loading: boolean;
}

const MySQLConfig: React.FC<MySQLConfigProps> = ({ config, onSave, loading }) => {
  const { t } = useTranslation();

  // 使用受控组件模式
  const [enabled, setEnabled] = useState(false);
  const [host, setHost] = useState('localhost');
  const [port, setPort] = useState(3306);
  const [database, setDatabase] = useState('luagin');
  const [username, setUsername] = useState('root');
  const [password, setPassword] = useState('');
  const [poolSize, setPoolSize] = useState(10);

  // 同步config变化到state
  useEffect(() => {
    if (config && typeof config === 'object') {
      if ('enabled' in config) {
        setEnabled(config.enabled);
      }
      if ('host' in config) {
        setHost(config.host);
      }
      if ('port' in config) {
        setPort(config.port);
      }
      if ('database' in config) {
        setDatabase(config.database);
      }
      if ('username' in config) {
        setUsername(config.username);
      }
      if ('password' in config) {
        setPassword(config.password);
      }
      if ('pool-size' in config) {
        setPoolSize(config['pool-size']);
      }
    }
  }, [config]);



  const handleSave = async () => {
    if (!host.trim()) {
      return;
    }
    if (port < 1 || port > 65535) {
      return;
    }
    if (!database.trim()) {
      return;
    }
    if (!username.trim()) {
      return;
    }

    // 转换字段名以匹配后端期望的格式
    const formattedValues = {
      enabled,
      host: host.trim(),
      port,
      database: database.trim(),
      username: username.trim(),
      password: password.trim(),
      'pool-size': poolSize,
    };
    onSave(formattedValues);
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
        message={t('config.mysqlConfig.description')}
        description={t('config.mysqlConfig.descriptionDetail')}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />



      <Form layout="vertical">
        <Card style={cardStyle}>
          <Title level={4}>
            <DatabaseOutlined style={{ marginRight: 8 }} />
            {t('config.mysqlConfig.basic')}
          </Title>
          
          <Form.Item
            label={t('config.mysqlConfig.enabled')}
          >
            <Switch
              checked={enabled}
              onChange={setEnabled}
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.connection')}</Title>
          
          <Form.Item
            label={t('config.mysqlConfig.host')}
          >
            <Input
              value={host}
              onChange={(e) => setHost(e.target.value)}
              placeholder={t('config.mysqlConfig.hostPlaceholder')}
            />
          </Form.Item>

          <Form.Item
            label={t('config.mysqlConfig.port')}
          >
            <InputNumber
              value={port}
              onChange={(value) => setPort(value || 3306)}
              placeholder={t('config.mysqlConfig.portPlaceholder')}
              style={{ width: '100%' }}
              min={1}
              max={65535}
            />
          </Form.Item>

          <Form.Item
            label={t('config.mysqlConfig.database')}
          >
            <Input
              value={database}
              onChange={(e) => setDatabase(e.target.value)}
              placeholder={t('config.mysqlConfig.databasePlaceholder')}
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.credentials')}</Title>

          <Form.Item
            label={t('config.mysqlConfig.username')}
          >
            <Input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder={t('config.mysqlConfig.usernamePlaceholder')}
            />
          </Form.Item>

          <Form.Item
            label={t('config.mysqlConfig.password')}
          >
            <Input.Password
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t('config.mysqlConfig.passwordPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.advanced')}</Title>
          
          <Form.Item
            label={t('config.mysqlConfig.poolSize')}
            extra={t('config.mysqlConfig.poolSizeHelp')}
          >
            <InputNumber
              value={poolSize}
              onChange={(value) => setPoolSize(value || 10)}
              placeholder={t('config.mysqlConfig.poolSizePlaceholder')}
              style={{ width: '100%' }}
              min={1}
              max={100}
            />
          </Form.Item>
        </Card>

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={loading}
              size="large"
            >
              {t('config.save')}
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  );
};

export default MySQLConfig; 