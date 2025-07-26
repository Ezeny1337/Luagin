import React, { useEffect } from 'react';
import { Form, Input, Switch, Button, Card, Space, Typography, Alert } from 'antd';
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
  const [form] = Form.useForm();

  useEffect(() => {
    if (config) {
      form.setFieldsValue({
        enabled: config.enabled ?? true,
        auth: {
          enabled: config.auth?.enabled ?? true,
          username: config.auth?.username ?? 'admin',
          password: config.auth?.password ?? 'admin',
          jwtSecret: config.auth?.jwtSecret ?? 'LuaginDefaultSecret',
        },
      });
    }
  }, [config, form]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      onSave(values);
    } catch (error) {
      console.error('Validation failed:', error);
    }
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

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          enabled: true,
          auth: {
            enabled: true,
            username: 'admin',
            password: 'admin',
            jwtSecret: 'LuaginDefaultSecret',
          },
        }}
      >
        <Card style={cardStyle}>
          <Title level={4}>{t('config.webpanelConfig.basic')}</Title>
          <Form.Item
            name="enabled"
            label={t('config.webpanelConfig.enabled')}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.webpanelConfig.auth')}</Title>
          
          <Form.Item
            name={['auth', 'enabled']}
            label={t('config.webpanelConfig.authEnabled')}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            name={['auth', 'username']}
            label={t('config.webpanelConfig.username')}
            rules={[
              { required: true, message: t('config.webpanelConfig.usernameRequired') },
              { min: 3, message: t('config.webpanelConfig.usernameMinLength') },
            ]}
          >
            <Input placeholder={t('config.webpanelConfig.usernamePlaceholder')} />
          </Form.Item>

          <Form.Item
            name={['auth', 'password']}
            label={t('config.webpanelConfig.password')}
            rules={[
              { required: true, message: t('config.webpanelConfig.passwordRequired') },
              { min: 6, message: t('config.webpanelConfig.passwordMinLength') },
            ]}
          >
            <Input.Password
              placeholder={t('config.webpanelConfig.passwordPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>

          <Form.Item
            name={['auth', 'jwtSecret']}
            label={t('config.webpanelConfig.jwtSecret')}
            rules={[
              { required: true, message: t('config.webpanelConfig.jwtSecretRequired') },
              { min: 16, message: t('config.webpanelConfig.jwtSecretMinLength') },
            ]}
            extra={t('config.webpanelConfig.jwtSecretHelp')}
          >
            <Input.Password
              placeholder={t('config.webpanelConfig.jwtSecretPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
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