import React, { useEffect, useState } from 'react';
import { Form, Input, Switch, Button, Card, Space, Typography, Alert, InputNumber } from 'antd';
import { SaveOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;

interface WebPanelConfigProps {
  config: any;
  onSave: (data: any) => void;
  loading: boolean;
}

const WebPanelConfig: React.FC<WebPanelConfigProps> = ({ config, onSave, loading }) => {
  const { t } = useTranslation();

  // 使用受控组件模式
  const [enabled, setEnabled] = useState(true);
  const [authEnabled, setAuthEnabled] = useState(true);
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');
  const [jwtSecret, setJwtSecret] = useState('LuaginDefaultSecret');
  const [cookieExpiry, setCookieExpiry] = useState(30);

  // 同步config变化到state
  useEffect(() => {
    if (config && typeof config === 'object') {
      if ('enabled' in config) {
        setEnabled(config.enabled);
      }
      if (config.auth && typeof config.auth === 'object') {
        if ('enabled' in config.auth) {
          setAuthEnabled(config.auth.enabled);
        }
        if ('username' in config.auth) {
          setUsername(config.auth.username);
        }
        if ('password' in config.auth) {
          setPassword(config.auth.password);
        }
        if ('jwtSecret' in config.auth) {
          setJwtSecret(config.auth.jwtSecret);
        }
        if ('cookieExpiry' in config.auth) {
          setCookieExpiry(config.auth.cookieExpiry);
        }
      }
    }
  }, [config]);

  const handleSave = async () => {
    if (!username.trim() || username.length < 3) {
      return;
    }
    if (!password.trim() || password.length < 6) {
      return;
    }
    if (!jwtSecret.trim() || jwtSecret.length < 16) {
      return;
    }

    const values = {
      enabled,
      auth: {
        enabled: authEnabled,
        username: username.trim(),
        password: password.trim(),
        jwtSecret: jwtSecret.trim(),
        cookieExpiry,
      },
    };
    onSave(values);
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
        message={t('config.webpanelConfig.description')}
        description={t('config.webpanelConfig.descriptionDetail')}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Form layout="vertical">
        <Card style={cardStyle}>
          <Title level={4}>{t('config.webpanelConfig.basic')}</Title>
          <Form.Item
            label={t('config.webpanelConfig.enabled')}
          >
            <Switch
              checked={enabled}
              onChange={setEnabled}
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.webpanelConfig.auth')}</Title>
          
          <Form.Item
            label={t('config.webpanelConfig.authEnabled')}
          >
            <Switch
              checked={authEnabled}
              onChange={setAuthEnabled}
            />
          </Form.Item>

          <Form.Item
            label={t('config.webpanelConfig.username')}
          >
            <Input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder={t('config.webpanelConfig.usernamePlaceholder')}
            />
          </Form.Item>

          <Form.Item
            label={t('config.webpanelConfig.password')}
          >
            <Input.Password
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t('config.webpanelConfig.passwordPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>

          <Form.Item
            label={t('config.webpanelConfig.jwtSecret')}
            extra={t('config.webpanelConfig.jwtSecretHelp')}
          >
            <Input.Password
              value={jwtSecret}
              onChange={(e) => setJwtSecret(e.target.value)}
              placeholder={t('config.webpanelConfig.jwtSecretPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>

          <Form.Item
            label={t('config.webpanelConfig.cookieExpiry', 'Cookie Expiry Time (minutes)')}
            extra={t('config.webpanelConfig.cookieExpiryHelp', 'How long the login session will last. Default is 30 minutes.')}
          >
            <InputNumber
              value={cookieExpiry}
              onChange={(value) => setCookieExpiry(value || 30)}
              min={1}
              max={10080}
              style={{ width: '100%' }}
              placeholder={t('config.webpanelConfig.cookieExpiryPlaceholder', '30')}
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

export default WebPanelConfig; 